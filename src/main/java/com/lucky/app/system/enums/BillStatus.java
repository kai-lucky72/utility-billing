package com.lucky.app.system.enums;

/** Lifecycle of a bill from creation through approval, payment, overdue, or cancellation. */
public enum BillStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED
}
