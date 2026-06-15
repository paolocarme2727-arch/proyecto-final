package com.example.bank.gateway.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues demo JWT tokens for protected APIs.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtEncoder jwtEncoder;

    /**
     * Creates the controller.
     *
     * @param jwtEncoder JWT encoder
     */
    public AuthController(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Issues a signed JWT for a subject.
     *
     * @param request token request
     * @return token response
     */
    @PostMapping("/token")
    public Map<String, String> token(@RequestBody TokenRequest request) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(request.username())
                .issuer("banking-platform")
                .issuedAt(now)
                .expiresAt(now.plus(2, ChronoUnit.HOURS))
                .claim("scope", "banking:read banking:write")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return Map.of("accessToken", token, "tokenType", "Bearer");
    }

    /**
     * Token request payload.
     */
    public record TokenRequest(String username) {
    }
}
