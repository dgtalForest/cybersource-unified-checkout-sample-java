package com.cybersource.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.cybersource.example.config.ApplicationProperties;
import com.cybersource.example.domain.CaptureContextResponseBody;
import com.cybersource.example.domain.JWK;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtProcessorService {

    @Value("classpath:keystore/private_key.pem")
    private Resource ctpDropInUIKey;
    @Autowired
    private final ApplicationProperties applicationProperties;
    @SneakyThrows
    public String verifyJwtAndGetDecodedBody(final String jwt) {
        // Parse the JWT response into header, payload, and signature
        final String[] jwtChunks = jwt.split("\\.");
        final Decoder decoder = Base64.getUrlDecoder();
        final String header = new String(decoder.decode(jwtChunks[0]));
        final String body = new String(decoder.decode(jwtChunks[1]));

        // Normally you'd want to cache the header and JWK, and only hit /flex/v2/public-keys/{kid} when the key rotates.
        // For simplicity and demonstration's sake let's retrieve it every time
        final JWK publicKeyJWK = getPublicKeyFromHeader(header);

        // Construct an RSA Key out of the response we got from the /public-keys endpoint
        final BigInteger modulus = new BigInteger(1, decoder.decode(publicKeyJWK.n()));
        final BigInteger exponent = new BigInteger(1, decoder.decode(publicKeyJWK.e()));
        final RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));

        // Verify the JWT's signature using the public key
        final Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, null);
        final JWTVerifier verifier = JWT.require(algorithm).acceptLeeway(20).build();

        // This will throw a runtime exception if there's a signature mismatch, we'd want this to blow up as it means the response
        // may be malicious and we can't proceed safely.
        verifier.verify(jwt);

        return body;
    }
    @SneakyThrows
    public String getClientVersionFromDecodedBody(final String jwtBody) {
        // Map the JWT Body to a POJO, which is probably the most readable way to do the stream below
        final CaptureContextResponseBody mappedBody = new ObjectMapper().readValue(jwtBody, CaptureContextResponseBody.class);

        // Dynamically retrieve the client library
        return mappedBody.ctx().stream().findFirst()
                .map(wrapper -> wrapper.data().clientLibrary())
                .orElseThrow();
    }

    @SneakyThrows
    public String decryptPaymentCredentials(final String paymentCredentials) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ctpDropInUIKey.getInputStream()))) {
            final String key = reader.lines()
                    .filter(line -> !line.startsWith("-----BEGIN PRIVATE KEY-----")
                            && !line.startsWith("-----END PRIVATE KEY-----"))
                    .collect(Collectors.joining());
            final byte[] keyBytes = Base64.getDecoder().decode(key);

            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            final JWEObject jweObject = JWEObject.parse(paymentCredentials);
            final JWEDecrypter decrypter = new RSADecrypter(privateKey);

            // This will throw an exception if the key doesn't match, etc.
            jweObject.decrypt(decrypter);

            final Payload decodedJWT = SignedJWT.parse(jweObject.getPayload().toString()).getPayload();

            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(decodedJWT.toJSONObject());
        }
    }

    @SneakyThrows
    private JWK getPublicKeyFromHeader(final String jwtHeader) {
        // Again, this process should be cached so you don't need to hit /public-keys every request
        // You'd want to look for a difference in the header's value (e.g. new key id [kid]) to refresh your cache
        final String keyId = new ObjectMapper().readTree(jwtHeader)
                .get("kid")
                .textValue();

        final ResponseEntity<String> response = new RestTemplate().getForEntity(
                        "https://" + applicationProperties.getRequestHost() + "/flex/v2/public-keys/" + keyId,
                        String.class);
        return new ObjectMapper().readValue(response.getBody(), JWK.class);
    }
}
