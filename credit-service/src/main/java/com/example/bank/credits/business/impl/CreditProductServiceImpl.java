package com.example.bank.credits.business.impl;

import com.example.bank.credits.business.CreditProductService;
import com.example.bank.credits.business.domain.CreditProductDomainService;
import com.example.bank.credits.business.impl.strategy.CreditMovementValidationStrategy;
import com.example.bank.credits.business.impl.strategy.CreditProductCreationValidationStrategy;
import com.example.bank.credits.business.impl.strategy.CreditProductShapeValidationStrategy;
import com.example.bank.credits.business.util.CreditBusinessUtils;
import com.example.bank.credits.domain.CreditMovement;
import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.events.CreditDebtStatusPublisher;
import com.example.bank.credits.expose.model.CreditBalanceResponse;
import com.example.bank.credits.expose.model.CreditCardExistenceResponse;
import com.example.bank.credits.expose.model.CreditProductReportResponse;
import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.expose.model.MoneyRequest;
import com.example.bank.credits.repository.CreditMovementRepository;
import com.example.bank.credits.repository.CreditProductRepository;
import com.example.bank.credits.util.enums.CreditMovementType;
import com.example.bank.credits.util.enums.CreditProductType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements credit CRUD operations, payments and credit card charges.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditProductServiceImpl implements CreditProductService {

    private final CreditProductRepository productRepository;
    private final CreditMovementRepository movementRepository;
    private final CreditDebtStatusPublisher debtStatusPublisher;
    private final CreditProductDomainService creditProductDomainService;
    private final List<CreditProductShapeValidationStrategy> shapeValidationStrategies;
    private final List<CreditProductCreationValidationStrategy> creationValidationStrategies;
    private final List<CreditMovementValidationStrategy> movementValidationStrategies;

    /**
     * Creates a product after validating credit business rules.
     */
    @Override
    public Single<CreditProduct> create(CreditProductRequest request) {
        return validateNewProduct(request)
                .andThen(Single.fromCallable(() ->
                        registerInitialDisbursement(productRepository.save(toNewProduct(request)))))
                .flatMap(product -> publishDebtStatus(product).andThen(Single.just(product)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(product -> log.info("Producto de crédito creado {}", product.getId()));
    }

    /**
     * Returns all credit products.
     */
    @Override
    public Flowable<CreditProduct> findAll() {
        return Single.fromCallable(productRepository::findAll)
                .subscribeOn(Schedulers.io())
                .flattenAsFlowable(products -> products);
    }

    /**
     * Returns one product or raises a 404 error.
     */
    @Override
    public Single<CreditProduct> findById(String id) {
        return creditProductDomainService.findExistingProductReactive(id);
    }

    /**
     * Updates product metadata while preserving current debt usage.
     */
    @Override
    public Single<CreditProduct> update(String id, CreditProductRequest request) {
        return creditProductDomainService.findExistingProductReactive(id)
                .flatMap(product -> Single.fromCallable(() -> {
                    validateProductRequest(request);
                    return productRepository.save(applyProductUpdate(product, request));
                }))
                .flatMap(product -> publishDebtStatus(product).andThen(Single.just(product)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(product -> log.info("Producto de crédito actualizado {}", product.getId()));
    }

    /**
     * Deletes a credit product if it exists.
     */
    @Override
    public Single<Boolean> delete(String id) {
        return creditProductDomainService.findExistingProductReactive(id)
                .flatMap(product -> Single.fromCallable(() -> {
                    productRepository.delete(product);
                    return Boolean.TRUE;
                }))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(ignored -> log.info("Producto de crédito eliminado {}", id));
    }

    /**
     * Pays down outstanding debt.
     */
    @Override
    public Single<CreditMovement> pay(String id, MoneyRequest request) {
        return creditProductDomainService.findExistingProductReactive(id)
                .flatMap(product -> Single.fromCallable(() ->
                        saveCreditMovement(product, request.getAmount(), CreditMovementType.PAYMENT)))
                .flatMap(movement -> creditProductDomainService.findExistingProductReactive(id)
                        .flatMap(product -> publishDebtStatus(product).andThen(Single.just(movement))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Pago registrado en el producto {}", id));
    }

    /**
     * Charges a credit card when the limit allows it.
     */
    @Override
    public Single<CreditMovement> charge(String id, MoneyRequest request) {
        return creditProductDomainService.findExistingProductReactive(id)
                .flatMap(product -> Single.fromCallable(() ->
                        saveCreditMovement(product, request.getAmount(), CreditMovementType.CHARGE)))
                .flatMap(movement -> creditProductDomainService.findExistingProductReactive(id)
                        .flatMap(product -> publishDebtStatus(product).andThen(Single.just(movement))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Consumo con tarjeta registrado en el producto {}", id));
    }

    /**
     * Returns outstanding debt and available credit.
     */
    @Override
    public Single<CreditBalanceResponse> getBalance(String id) {
        return creditProductDomainService.findExistingProductReactive(id).map(this::toBalance);
    }

    /**
     * Returns all movements for one product.
     */
    @Override
    public Flowable<CreditMovement> findMovements(String id) {
        return Single.fromCallable(() -> {
                    CreditProduct product = creditProductDomainService.findExistingProduct(id);
                    return movementRepository.findByCreditProductIdOrderByCreatedAtDesc(product.getId());
                })
                .flattenAsFlowable(movements -> movements)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Checks credit card ownership for cross-service product rules.
     */
    @Override
    public Single<CreditCardExistenceResponse> customerHasCreditCard(String customerId) {
        return Single.fromCallable(() -> productRepository.existsByCustomerIdAndTypeIn(
                        customerId,
                        List.of(CreditProductType.PERSONAL_CREDIT_CARD, CreditProductType.BUSINESS_CREDIT_CARD)))
                .map(hasCard -> new CreditCardExistenceResponse().customerId(customerId).hasCreditCard(hasCard))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Generates a complete credit report by product type and time interval.
     */
    @Override
    public Single<CreditProductReportResponse> getProductReport(
            com.example.bank.credits.expose.model.CreditProductType type,
            OffsetDateTime from,
            OffsetDateTime to) {
        return Single.fromCallable(() -> {
                    CreditProductType productType = CreditProductType.valueOf(type.getValue());
                    List<CreditProduct> products = productRepository.findByType(productType);
                    List<String> productIds = products.stream().map(CreditProduct::getId).toList();
                    List<CreditMovement> movements = productIds.isEmpty()
                            ? List.of()
                            : movementRepository.findByCreditProductIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
                                    productIds,
                                    from.toLocalDateTime(),
                                    to.toLocalDateTime());
                    return toCreditReport(type, from, to, products.size(), movements);
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * Lists the latest movements for a credit card.
     */
    @Override
    public Flowable<CreditMovement> findRecentCreditCardMovements(String id) {
        return Single.fromCallable(() -> {
                    CreditProduct product = creditProductDomainService.findExistingProduct(id);
                    validateCardProduct(product);
                    return movementRepository.findByCreditProductIdOrderByCreatedAtDesc(
                            product.getId(),
                            PageRequest.of(0, 10));
                })
                .flattenAsFlowable(movements -> movements)
                .subscribeOn(Schedulers.io());
    }

    private Completable validateNewProduct(CreditProductRequest request) {
        return Completable.fromAction(() -> validateProductRequest(request))
                .andThen(Completable.merge(creationValidationStrategies.stream()
                        .map(strategy -> strategy.validate(request))
                        .toList()));
    }

    private void validateProductRequest(CreditProductRequest request) {
        shapeValidationStrategies.forEach(strategy -> strategy.validate(request));
    }

    private void validateCardProduct(CreditProduct product) {
        if (!CreditBusinessUtils.isCard(product.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La operación solo está permitida para tarjetas de crédito");
        }
    }

    private CreditMovement saveCreditMovement(CreditProduct product, BigDecimal amount, CreditMovementType type) {
        movementValidationStrategies.forEach(strategy -> strategy.validate(product, amount, type));
        BigDecimal signedAmount = type == CreditMovementType.PAYMENT ? amount.negate() : amount;
        CreditProduct saved = productRepository.save(applyCreditMovement(product, signedAmount));
        return movementRepository.save(toCreditMovement(saved, amount, type));
    }

    private CreditProduct applyCreditMovement(CreditProduct product, BigDecimal signedAmount) {
        product.setOutstandingBalance(product.getOutstandingBalance().add(signedAmount));
        if (CreditBusinessUtils.isCard(product.getType())) {
            product.setUsedAmount(product.getUsedAmount().add(signedAmount));
        }
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    private CreditMovement toCreditMovement(CreditProduct product, BigDecimal amount, CreditMovementType type) {
        return CreditMovement.builder()
                .creditProductId(product.getId())
                .type(type)
                .amount(amount)
                .resultingDebt(product.getOutstandingBalance())
                .availableCredit(calculateAvailableCredit(product))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CreditProduct registerInitialDisbursement(CreditProduct product) {
        if (product.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            movementRepository.save(toDisbursementMovement(product));
        }
        return product;
    }

    private CreditMovement toDisbursementMovement(CreditProduct product) {
        return CreditMovement.builder()
                .creditProductId(product.getId())
                .type(CreditMovementType.DISBURSEMENT)
                .amount(product.getOutstandingBalance())
                .resultingDebt(product.getOutstandingBalance())
                .availableCredit(calculateAvailableCredit(product))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CreditProduct toNewProduct(CreditProductRequest request) {
        validateProductRequest(request);
        BigDecimal initialDebt = CreditBusinessUtils.initialDebt(request);
        return CreditProduct.builder()
                .customerId(request.getCustomerId())
                .customerType(CreditBusinessUtils.toDomainCustomerType(request))
                .type(CreditBusinessUtils.toDomainProductType(request))
                .creditLimit(request.getCreditLimit())
                .usedAmount(CreditBusinessUtils.isCard(CreditBusinessUtils.toDomainProductType(request))
                        ? initialDebt
                        : BigDecimal.ZERO)
                .outstandingBalance(initialDebt)
                .overdueDebt(Boolean.TRUE.equals(request.getOverdueDebt()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CreditProduct applyProductUpdate(CreditProduct product, CreditProductRequest request) {
        product.setCustomerId(request.getCustomerId());
        product.setCustomerType(CreditBusinessUtils.toDomainCustomerType(request));
        product.setType(CreditBusinessUtils.toDomainProductType(request));
        product.setCreditLimit(request.getCreditLimit());
        product.setOverdueDebt(Boolean.TRUE.equals(request.getOverdueDebt()));
        product.setUpdatedAt(LocalDateTime.now());
        validateCurrentDebtCoverage(product);
        return product;
    }

    private void validateCurrentDebtCoverage(CreditProduct product) {
        if (product.getCreditLimit().compareTo(product.getOutstandingBalance()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El límite de crédito no puede ser menor que la deuda actual");
        }
    }

    private CreditBalanceResponse toBalance(CreditProduct product) {
        return new CreditBalanceResponse()
                .productId(product.getId())
                .outstandingBalance(product.getOutstandingBalance())
                .availableCredit(calculateAvailableCredit(product));
    }

    private Completable publishDebtStatus(CreditProduct product) {
        return Single.fromCallable(() -> productRepository.existsByCustomerIdAndOverdueDebtTrue(product.getCustomerId()))
                .flatMapCompletable(hasDebt -> debtStatusPublisher.publish(product.getCustomerId(), hasDebt));
    }

    private BigDecimal calculateAvailableCredit(CreditProduct product) {
        return CreditBusinessUtils.isCard(product.getType())
                ? product.getCreditLimit().subtract(product.getUsedAmount())
                : BigDecimal.ZERO;
    }

    private CreditProductReportResponse toCreditReport(
            com.example.bank.credits.expose.model.CreditProductType type,
            OffsetDateTime from,
            OffsetDateTime to,
            int productCount,
            List<CreditMovement> movements) {
        BigDecimal totalAmount = movements.stream()
                .map(CreditMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<String> productIds = movements.stream()
                .map(CreditMovement::getCreditProductId)
                .collect(Collectors.toSet());
        return new CreditProductReportResponse()
                .productType(type)
                .from(from)
                .to(to)
                .productCount(productCount == 0 ? productIds.size() : productCount)
                .movementCount(movements.size())
                .totalAmount(totalAmount)
                .movements(movements.stream().map(this::toApiMovement).toList());
    }

    private com.example.bank.credits.expose.model.CreditMovement toApiMovement(CreditMovement movement) {
        return new com.example.bank.credits.expose.model.CreditMovement()
                .id(movement.getId())
                .creditProductId(movement.getCreditProductId())
                .type(com.example.bank.credits.expose.model.CreditMovementType.fromValue(movement.getType().name()))
                .amount(movement.getAmount())
                .resultingDebt(movement.getResultingDebt())
                .availableCredit(movement.getAvailableCredit())
                .createdAt(movement.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
