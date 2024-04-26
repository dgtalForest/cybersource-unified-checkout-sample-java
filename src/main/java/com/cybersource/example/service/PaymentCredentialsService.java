package com.cybersource.example.service;

import com.cybersource.example.config.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Log4j2
@RequiredArgsConstructor
public class PaymentCredentialsService {
    @Autowired
    private final ApplicationProperties applicationProperties;

    @SneakyThrows
    public String getEncryptedPaymentCredentials(final String jwt) {
        // Create an instance of Cybersource's generic API client using our merchant config.
        // In this case we want to use the portfolio ID since only the portfolio has access to access and decrypt the payment credentials
        final String paymentCredentialsReferenceId = new ObjectMapper().readTree(jwt).get("paymentCredentialsReference").get(applicationProperties.getPorfolioId()).textValue();

        // TODO: Use SDK when endpoint is updated and remove this method.
        return getEncryptedPaymentCredentialsWithoutSDK(paymentCredentialsReferenceId);
    }

    @SneakyThrows
    public String getEncryptedPaymentCredentialsWithoutSDK(final String paymentCredentialsReference) {

        final String date = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")));
        final String resource = "/flex/v2/payment-credentials/" + paymentCredentialsReference;
        final String url = "https://" + applicationProperties.getRequestHost() + resource;

        // All of these headers are created automatically by the API client in the above method.
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.set("v-c-merchant-id", applicationProperties.getPorfolioId());
        headers.set("date", date);
        headers.set("host", applicationProperties.getRequestHost());
        headers.set("signature", getSignatureHeader(date, applicationProperties, resource));
        headers.set("User-Agent", applicationProperties.getUserAgent());
        final HttpEntity<String> request = new HttpEntity<>(headers);

        final RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(url, GET, request, String.class).getBody();
    }

    private String getSignatureHeader(final String date, final ApplicationProperties properties, final String resource) {
        return """
                keyid="%s", algorithm="HmacSHA256", \
                headers="host date (request-target) v-c-merchant-id", signature="%s"\
                """
                .formatted(properties.getPortfolioKeyId(), getSignatureParam(date, applicationProperties, resource));
    }

    @SneakyThrows
    private String getSignatureParam(final String date, final ApplicationProperties properties, final String resource) {
        final String signatureString = """
                host: %s
                date: %s
                (request-target): %s
                v-c-merchant-id: %s\
                """.formatted(properties.getRequestHost(), date, "get " + resource, properties.getPorfolioId());
        final SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(properties.getPortfolioSecretKey()), "HmacSHA256");
        final Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        hmacSHA256.init(secretKey);
        hmacSHA256.update(signatureString.getBytes());
        return Base64.getEncoder().encodeToString(hmacSHA256.doFinal());
    }

}
