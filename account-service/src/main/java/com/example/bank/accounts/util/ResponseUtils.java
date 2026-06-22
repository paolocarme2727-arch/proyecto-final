package com.example.bank.accounts.util;

import io.reactivex.rxjava3.core.Single;
import lombok.experimental.UtilityClass;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

@UtilityClass
public class ResponseUtils {

    @SuppressWarnings("unchecked")
    public static <T> ResponseEntity<T> resolve(Supplier<Single<ResponseEntity<T>>> responseSupplier) {
        try {
            return responseSupplier.get().blockingGet();
        } catch (RuntimeException error) {
            ResponseStatusException exception = findStatusException(error);
            if (exception != null) {
                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                        exception.getStatusCode(),
                        exception.getReason());
                problemDetail.setTitle("Error en la solicitud");
                return (ResponseEntity<T>) ResponseEntity.status(exception.getStatusCode()).body(problemDetail);
            }
            throw error;
        }
    }

    private static ResponseStatusException findStatusException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResponseStatusException exception) {
                return exception;
            }
            current = current.getCause();
        }
        return null;
    }

}
