package se.sundsvall.supportcenter.integration.pob.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.pob")
public record POBProperties(int connectTimeout, int readTimeout) {
}
