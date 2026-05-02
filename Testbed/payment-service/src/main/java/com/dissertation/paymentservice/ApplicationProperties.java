package com.dissertation.paymentservice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "payment")
@Validated
public record ApplicationProperties(
) {
}
