# ERD

```mermaid
erDiagram
    USERS ||--o| CUSTOMERS : "links optional customer profile"
    CUSTOMERS ||--o{ METERS : owns
    METERS ||--o{ METER_READINGS : records
    METER_READINGS ||--|| BILLS : generates
    CUSTOMERS ||--o{ BILLS : receives
    BILLS ||--o{ PAYMENTS : settles
    CUSTOMERS ||--o{ NOTIFICATIONS : receives
    BILLS ||--o{ NOTIFICATIONS : references
    TARIFFS ||--o{ TARIFF_TIERS : contains
    TARIFFS ||--o{ BILLS : "pricing snapshot source"
```

## Notes

- Financial records are retained; bills and payments are not physically deleted.
- Customer-facing access is resolved from the authenticated user and linked customer profile.
- Money fields use `BigDecimal` in Java and fixed precision numeric columns in the database.
