package com.example.bank.gateway;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Servlet based API gateway proxy.
 */
@RestController
public class GatewayProxyController {

    private final RestClient.Builder restClientBuilder;
    private final Map<String, String> serviceUrlBySegment;

    /**
     * Creates the proxy controller.
     *
     * @param restClientBuilder RestClient builder
     * @param customerServiceUrl customer service base URL
     * @param accountServiceUrl account service base URL
     * @param creditServiceUrl credit service base URL
     * @param walletServiceUrl wallet service base URL
     */
    public GatewayProxyController(
            RestClient.Builder restClientBuilder,
            @Value("${banking.services.customer-url:http://localhost:8081}") String customerServiceUrl,
            @Value("${banking.services.account-url:http://localhost:8082}") String accountServiceUrl,
            @Value("${banking.services.credit-url:http://localhost:8083}") String creditServiceUrl,
            @Value("${banking.services.wallet-url:http://localhost:8084}") String walletServiceUrl) {
        this.restClientBuilder = restClientBuilder;
        this.serviceUrlBySegment = Map.of(
                "customers", customerServiceUrl,
                "accounts", accountServiceUrl,
                "credits", creditServiceUrl,
                "wallets", walletServiceUrl);
    }

    /**
     * Proxies API requests to the owning microservice.
     *
     * @param body request body
     * @param request servlet request
     * @return downstream response
     * @throws IOException when the body cannot be copied
     */
    @RequestMapping("/api/v1/{segment}/**")
    public ResponseEntity<byte[]> proxy(@RequestBody(required = false) byte[] body, HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String segment = path.substring("/api/v1/".length()).split("/", 2)[0];
        String serviceUrl = serviceUrlBySegment.get(segment);
        if (serviceUrl == null) {
            return ResponseEntity.notFound().build();
        }
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        String target = serviceUrl + path + query;
        return restClientBuilder.build()
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(URI.create(target))
                .headers(headers -> copyHeaders(request, headers))
                .body(body == null ? new byte[0] : body)
                .exchange((downstreamRequest, downstreamResponse) -> ResponseEntity
                        .status(downstreamResponse.getStatusCode())
                        .headers(downstreamResponse.getHeaders())
                        .body(downstreamResponse.getBody().readAllBytes()));
    }

    private void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        Collections.list(request.getHeaderNames()).stream()
                .filter(header -> !header.equalsIgnoreCase(HttpHeaders.HOST))
                .filter(header -> !header.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH))
                .forEach(header -> headers.addAll(header, Collections.list(request.getHeaders(header))));
    }
}
