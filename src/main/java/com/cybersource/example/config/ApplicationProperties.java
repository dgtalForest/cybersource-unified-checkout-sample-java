package com.cybersource.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Data
@Component
@ConfigurationProperties("app")
// Spring automatically looks for an application.properties in src/main/resources and binds to corresponding fields.
public class ApplicationProperties {
    String merchantId;
    String merchantKeyId;
    String merchantSecretKey;
    String porfolioId;
    String portfolioKeyId;
    String portfolioSecretKey;
    String requestHost;
    String userAgent;
    String runEnvironment;
    String authenticationType;

    public Properties getMerchantConfigAsJavaProperties() {
        Properties props = getSharedProperties();
        props.setProperty("merchantID", getMerchantId());
        props.setProperty("merchantKeyId", getMerchantKeyId());
        props.setProperty("merchantsecretKey", getMerchantSecretKey());
        return props;
    }

    // When we need to assume the role of the portfolio, who encrypts the payment credentials, we must use a different Merchant config
    // With the portfolio's information
    // Currently not used but will be when the SDK is fixed
    public Properties getPortfolioConfigAsJavaProperties() {
        Properties props = getSharedProperties();
        props.setProperty("merchantID", getPorfolioId());
        props.setProperty("merchantKeyId", getPortfolioKeyId());
        props.setProperty("merchantsecretKey", getPortfolioSecretKey());
        return props;
    }

    private Properties getSharedProperties() {
        Properties props = new Properties();
        props.setProperty("userAgent", getUserAgent());
        props.setProperty("requestHost", requestHost);
        props.setProperty("runEnvironment", getRunEnvironment());
        props.setProperty("authenticationType", getAuthenticationType());
        return props;
    }
}
