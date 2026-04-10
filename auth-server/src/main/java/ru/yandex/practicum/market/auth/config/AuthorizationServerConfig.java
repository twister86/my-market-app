package ru.yandex.practicum.market.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Value("${auth-server.issuer}")
    private String issuer;

    @Value("${auth.client.secret:market-secret}")
    private String clientSecret;

    @Value("${auth.client.key:market-key-1}")
    private String keyId;

    @Bean
    public PasswordEncoder authPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder authPasswordEncoder) {
        RegisteredClient marketClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("market-app")
                // Секрет хранится в зашифрованном виде (BCrypt)
                .clientSecret(authPasswordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payment.read")
                .scope("payment.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(marketClient);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    /**
     * RSA JWK для подписи JWT.
     *
     * Если в конфиге заданы ключи (PEM-строки) — загружаем их.
     * Иначе генерируем новую пару (только для dev, при перезапуске токены инвалидируются).
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(
            @Value("${auth.rsa.public-key:}") String publicKeyPem,
            @Value("${auth.rsa.private-key:}") String privateKeyPem) throws Exception {

        KeyPair keyPair;
        if (!publicKeyPem.isBlank() && !privateKeyPem.isBlank()) {
            keyPair = loadKeyPair(publicKeyPem, privateKeyPem);
        } else {
            // Dev-режим: генерируем ключи при старте.
            // ВНИМАНИЕ: после перезапуска ранее выданные токены станут невалидными.
            keyPair = generateKeyPair();
        }

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(keyId)
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    // ===== Вспомогательные методы =====

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /**
     * Загружает RSA ключи из PEM-строк (Base64 без заголовков).
     * В application.properties задаётся как:
     *   auth.rsa.public-key=MIIBIjAN...
     *   auth.rsa.private-key=MIIEvQIB...
     */
    private KeyPair loadKeyPair(String publicKeyPem, String privateKeyPem) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        byte[] pubBytes  = Base64.getDecoder().decode(publicKeyPem.replaceAll("\\s", ""));
        byte[] privBytes = Base64.getDecoder().decode(privateKeyPem.replaceAll("\\s", ""));

        RSAPublicKey  publicKey  = (RSAPublicKey)  kf.generatePublic(new X509EncodedKeySpec(pubBytes));
        RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

        return new KeyPair(publicKey, privateKey);
    }
}
