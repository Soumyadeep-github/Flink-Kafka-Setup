package org.example.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Transaction.java
public class Transaction {
    private String transactionId;
    private String userId;
    private double amount;
    private String timestamp;
    private String merchantId;
    private String category;

    // Getters, setters, and constructors
}