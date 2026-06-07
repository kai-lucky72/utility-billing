package com.lucky.app.system.util;

import java.time.LocalDate;

/** Builds human-readable, unique reference codes for bills (BILL-YYYY-MM-######) and payments (PAY-...). */
public final class ReferenceGenerator {

    private ReferenceGenerator() {
    }

    public static String billReference(LocalDate date, long sequence) {
        return "BILL-%d-%02d-%06d".formatted(date.getYear(), date.getMonthValue(), sequence);
    }

    public static String paymentReference(LocalDate date, long sequence) {
        return "PAY-%d-%02d-%06d".formatted(date.getYear(), date.getMonthValue(), sequence);
    }
}
