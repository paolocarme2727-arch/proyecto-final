package com.example.bank.wallets.service;

import com.example.bank.wallets.util.Constants;
import com.example.bank.wallets.util.enums.DocumentType;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Caches master data for document types in Redis.
 */
@Component
public class DocumentTypeCatalog {

    private final StringRedisTemplate redisTemplate;

    /**
     * Creates the catalog.
     *
     * @param redisTemplate Redis template
     */
    public DocumentTypeCatalog(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Validates a document type using Redis-backed master data.
     *
     * @param documentType document type
     * @return true when supported
     */
    public Single<Boolean> isSupported(DocumentType documentType) {
        return Single.fromCallable(() -> {
                    Long size = redisTemplate.opsForSet().size(Constants.DOCUMENT_TYPES_KEY);
                    if (size == null || size == 0) {
                        seedDocumentTypes();
                    }
                    return Boolean.TRUE.equals(redisTemplate.opsForSet()
                            .isMember(Constants.DOCUMENT_TYPES_KEY, documentType.name()));
                })
                .subscribeOn(Schedulers.io());
    }

    private Long seedDocumentTypes() {
        Set<String> values = Arrays.stream(DocumentType.values()).map(Enum::name).collect(Collectors.toSet());
        return redisTemplate.opsForSet().add(Constants.DOCUMENT_TYPES_KEY, values.toArray(String[]::new));
    }
}

