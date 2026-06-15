package com.example.bank.credits.business.impl;

import com.example.bank.credits.business.CreditProductService;
import com.example.bank.credits.domain.CreditMovement;
import com.example.bank.credits.domain.CreditMovementType;
import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.domain.CreditProductType;
import com.example.bank.credits.domain.CustomerType;
import com.example.bank.credits.events.CreditDebtStatusPublisher;
import com.example.bank.credits.expose.model.CreditBalanceResponse;
import com.example.bank.credits.expose.model.CreditCardExistenceResponse;
import com.example.bank.credits.expose.model.CreditProductReportResponse;
import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.expose.model.MoneyRequest;
import com.example.bank.credits.repository.CreditMovementRepository;
import com.example.bank.credits.repository.CreditProductRepository;
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

    /**
     * Creates a product after validating credit business rules.
     */
    @Override
    public Single<CreditProduct> create(CreditProductRequest request) {
        return validateNewProduct(request)
                .andThen(Single.fromCallable(() -> registerInitialDisbursement(productRepository.save(toNewProduct(request)))))
                .flatMap(product -> publishDebtStatus(product).andThen(Single.just(product)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(product -> log.info("Created credit product {}", product.getId()));
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
        return findExisting(id);
    }

    /**
     * Updates product metadata while preserving current debt usage.
     */
    @Override
    public Single<CreditProduct> update(String id, CreditProductRequest request) {
        return findExisting(id)
                .map(product -> {
                    validateProductRequest(request);
                    product.setCustomerId(request.getCustomerId());
                    product.setCustomerType(toDomainCustomerType(request));
                    product.setType(toDomainProductType(request));
                    product.setCreditLimit(request.getCreditLimit());
                    product.setOverdueDebt(Boolean.TRUE.equals(request.getOverdueDebt()));
                    product.setUpdatedAt(LocalDateTime.now());
                    validateProductShape(product);
                    return productRepository.save(product);
                })
                .flatMap(product -> publishDebtStatus(product).andThen(Single.just(product)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(product -> log.info("Updated credit product {}", product.getId()));
    }

    /**
     * Deletes a credit product if it exists.
     */
    @Override
    public Single<Boolean> delete(String id) {
        return findExisting(id)
                .map(product -> {
                    productRepository.delete(product);
                    return Boolean.TRUE;
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(ignored -> log.info("Deleted credit product {}", id));
    }

    /**
     * Pays down outstanding debt.
     */
    @Override
    public Single<CreditMovement> pay(String id, MoneyRequest request) {
        return findExisting(id)
                .map(product -> saveCreditMovement(product, request.getAmount(), CreditMovementType.PAYMENT))
                .flatMap(movement -> findExisting(id)
                        .flatMap(product -> publishDebtStatus(product).andThen(Single.just(movement))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Registered payment on product {}", id));
    }

    /**
     * Charges a credit card when the limit allows it.
     */
    @Override
    public Single<CreditMovement> charge(String id, MoneyRequest request) {
        return findExisting(id)
                .map(product -> {
                    validateCardCharge(product, request.getAmount());
                    return saveCreditMovement(product, request.getAmount(), CreditMovementType.CHARGE);
                })
                .flatMap(movement -> findExisting(id)
                        .flatMap(product -> publishDebtStatus(product).andThen(Single.just(movement))))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Registered card charge on product {}", id));
    }

    /**
     * Returns outstanding debt and available credit.
     */
    @Override
    public Single<CreditBalanceResponse> getBalance(String id) {
        return findExisting(id).map(this::toBalance);
    }

    /**
     * Returns all movements for one product.
     */
    @Override
    public Flowable<CreditMovement> findMovements(String id) {
        return findExisting(id)
                .map(product -> movementRepository.findByCreditProductIdOrderByCreatedAtDesc(product.getId()))
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
        return findExisting(id)
                .map(product -> {
                    validateCardProduct(product);
                    return movementRepository.findByCreditProductIdOrderByCreatedAtDesc(product.getId(), PageRequest.of(0, 10));
                })
                .flattenAsFlowable(movements -> movements)
                .subscribeOn(Schedulers.io());
    }

    private Completable validateNewProduct(CreditProductRequest request) {
        return Completable.fromAction(() -> {
            validateProductRequest(request);
            if (productRepository.existsByCustomerIdAndOverdueDebtTrue(request.getCustomerId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer has overdue credit debt");
            }
            if (toDomainCustomerType(request) == CustomerType.PERSONAL
                    && toDomainProductType(request) == CreditProductType.PERSONAL_LOAN
                    && productRepository.existsByCustomerIdAndType(request.getCustomerId(), CreditProductType.PERSONAL_LOAN)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Personal customer already has a personal loan");
            }
        });
    }

    private void validateProductRequest(CreditProductRequest request) {
        if (toDomainCustomerType(request) == CustomerType.PERSONAL
                && (toDomainProductType(request) == CreditProductType.BUSINESS_LOAN || toDomainProductType(request) == CreditProductType.BUSINESS_CREDIT_CARD)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Personal customers cannot own business credit products");
        }
        if (toDomainCustomerType(request) == CustomerType.BUSINESS
                && (toDomainProductType(request) == CreditProductType.PERSONAL_LOAN || toDomainProductType(request) == CreditProductType.PERSONAL_CREDIT_CARD)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business customers cannot own personal credit products");
        }
    }

    private void validateCardCharge(CreditProduct product, BigDecimal amount) {
        validateCardProduct(product);
        if (product.getUsedAmount().add(amount).compareTo(product.getCreditLimit()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit card limit exceeded");
        }
    }

    private void validateCardProduct(CreditProduct product) {
        if (!isCard(product.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation is allowed only for credit cards");
        }
    }

    private CreditMovement saveCreditMovement(CreditProduct product, BigDecimal amount, CreditMovementType type) {
        if (type == CreditMovementType.PAYMENT && product.getOutstandingBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds outstanding debt");
        }
        BigDecimal signedAmount = type == CreditMovementType.PAYMENT ? amount.negate() : amount;
        product.setOutstandingBalance(product.getOutstandingBalance().add(signedAmount));
        if (isCard(product.getType())) {
            product.setUsedAmount(product.getUsedAmount().add(signedAmount));
        }
        product.setUpdatedAt(LocalDateTime.now());
        CreditProduct saved = productRepository.save(product);
        return movementRepository.save(CreditMovement.builder()
                .creditProductId(saved.getId())
                .type(type)
                .amount(amount)
                .resultingDebt(saved.getOutstandingBalance())
                .availableCredit(calculateAvailableCredit(saved))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private CreditProduct registerInitialDisbursement(CreditProduct product) {
        if (product.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            movementRepository.save(CreditMovement.builder()
                    .creditProductId(product.getId())
                    .type(CreditMovementType.DISBURSEMENT)
                    .amount(product.getOutstandingBalance())
                    .resultingDebt(product.getOutstandingBalance())
                    .availableCredit(calculateAvailableCredit(product))
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return product;
    }

    private Single<CreditProduct> findExisting(String id) {
        return Single.fromCallable(() -> productRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit product not found")))
                .subscribeOn(Schedulers.io());
    }

    private CreditProduct toNewProduct(CreditProductRequest request) {
        validateProductRequest(request);
        BigDecimal initialDebt = request.getInitialDebt() == null ? BigDecimal.ZERO : request.getInitialDebt();
        CreditProduct product = CreditProduct.builder()
                .customerId(request.getCustomerId())
                .customerType(toDomainCustomerType(request))
                .type(toDomainProductType(request))
                .creditLimit(request.getCreditLimit())
                .usedAmount(isCard(toDomainProductType(request)) ? initialDebt : BigDecimal.ZERO)
                .outstandingBalance(initialDebt)
                .overdueDebt(Boolean.TRUE.equals(request.getOverdueDebt()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        validateProductShape(product);
        return product;
    }

    private void validateProductShape(CreditProduct product) {
        if (product.getCreditLimit().compareTo(product.getOutstandingBalance()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit limit cannot be lower than current debt");
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
        return isCard(product.getType()) ? product.getCreditLimit().subtract(product.getUsedAmount()) : BigDecimal.ZERO;
    }

    private boolean isCard(CreditProductType type) {
        return type == CreditProductType.PERSONAL_CREDIT_CARD || type == CreditProductType.BUSINESS_CREDIT_CARD;
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
        Set<String> productIds = movements.stream().map(CreditMovement::getCreditProductId).collect(java.util.stream.Collectors.toSet());
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

    private CustomerType toDomainCustomerType(CreditProductRequest request) {
        return CustomerType.valueOf(request.getCustomerType().getValue());
    }

    private CreditProductType toDomainProductType(CreditProductRequest request) {
        return CreditProductType.valueOf(request.getType().getValue());
    }
}


