package com.lucky.app.system.enums;

/** Security roles that gate API access: Admin, Operator (readings), Finance (bills/payments), Customer. */
public enum Role {
    ROLE_ADMIN,
    ROLE_OPERATOR,
    ROLE_FINANCE,
    ROLE_CUSTOMER
}
