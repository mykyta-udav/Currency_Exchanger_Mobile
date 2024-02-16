package com.example.curexch;

import java.math.BigDecimal;

public class CurrentExchanger {
    public static BigDecimal calculateExchangedAmount(BigDecimal amount, BigDecimal exchangeRate) {
        return amount.multiply(exchangeRate);
    }
}
