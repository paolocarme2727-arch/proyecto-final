package com.example.bank.gateway;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides a shared fallback response for gateway circuit breakers.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Handles fallback requests for any HTTP verb configured in gateway routes.
     *
     * @return fallback payload
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> fallback() {
        return Map.of("message", "El servicio no está disponible temporalmente");
    }
}
