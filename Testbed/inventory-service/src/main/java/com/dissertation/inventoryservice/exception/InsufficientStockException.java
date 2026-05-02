package com.dissertation.inventoryservice.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InsufficientStockException extends RuntimeException {
    private final List<String> missingItems;

    public InsufficientStockException(List<String> missingItems) {
        super("Insufficient stock for items: " + missingItems);
        this.missingItems = missingItems;
    }

}
