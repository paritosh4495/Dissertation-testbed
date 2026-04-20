package com.dissertation.orderservice;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "order")
@Validated
public record ApplicationProperties(
        @NotBlank
        String inventoryServiceUrl,

        @NotBlank
        String paymentServiceUrl
) {
}
