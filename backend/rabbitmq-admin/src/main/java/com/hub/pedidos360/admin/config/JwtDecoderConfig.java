package com.hub.pedidos360.admin.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Value("${azure.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${azure.v1-issuer}")
    private String v1Issuer;

    @Value("${azure.api-audience}")
    private String apiAudience;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
            JwtValidators.createDefaultWithIssuer(v1Issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            "aud", audiences -> audiences != null && audiences.contains(apiAudience));

        decoder.setJwtValidator(
            new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }
}
