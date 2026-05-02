package com.dissertation.inventoryservice;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "inventory")
@Validated
public record ApplicationProperties(
        @DefaultValue("10")
        @Min(1)
        int pageSize
) {
}
