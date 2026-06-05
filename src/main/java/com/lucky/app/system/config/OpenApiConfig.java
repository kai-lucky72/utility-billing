package com.lucky.app.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Utility Billing System API")
                        .version("v1")
                        .description("Secure utility billing backend for WASAC and REG exam project")
                        .contact(new Contact().name("OpenAI Codex")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer utilityBillingExamplesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                if (operation.getParameters() != null) {
                    operation.getParameters().forEach(this::applyParameterExamples);
                }

                if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
                    return;
                }

                MediaType jsonContent = operation.getRequestBody().getContent().get("application/json");
                if (jsonContent == null || jsonContent.getSchema() == null || jsonContent.getExample() != null) {
                    return;
                }

                String schemaRef = jsonContent.getSchema().get$ref();
                if (schemaRef == null) {
                    return;
                }

                String schemaName = schemaRef.substring(schemaRef.lastIndexOf('/') + 1);
                Object example = buildRequestExample(schemaName);
                if (example != null) {
                    jsonContent.setExample(example);
                }
            }));
        };
    }

    private void applyParameterExamples(Parameter parameter) {
        if (parameter.getSchema() == null) {
            return;
        }

        Schema<?> schema = parameter.getSchema();
        switch (parameter.getName()) {
            case "id" -> {
                schema.setExample(1L);
                applyDescription(parameter, "Primary resource id.");
            }
            case "customerId" -> {
                schema.setExample(1L);
                applyDescription(parameter, "Customer id, for example the seeded customer profile id.");
            }
            case "meterId" -> {
                schema.setExample(1L);
                applyDescription(parameter, "Meter id, for example a seeded water or electricity meter.");
            }
            case "readingId" -> {
                schema.setExample(1L);
                applyDescription(parameter, "Meter reading id generated after capturing a reading.");
            }
            case "billId" -> {
                schema.setExample(1L);
                applyDescription(parameter, "Bill id returned from the bills endpoints.");
            }
            case "billReference" -> {
                schema.setExample("BILL-2026-03-000001");
                applyDescription(parameter, "Human-readable bill reference.");
            }
            case "month" -> {
                schema.setExample(3);
                applyDescription(parameter, "Billing month from 1 to 12.");
            }
            case "year" -> {
                schema.setExample(2026);
                applyDescription(parameter, "Four-digit billing year.");
            }
            case "page" -> {
                schema.setExample(0);
                applyDescription(parameter, "Zero-based page number.");
            }
            case "size" -> {
                schema.setExample(10);
                applyDescription(parameter, "Number of records to return.");
            }
            default -> {
            }
        }
    }

    private void applyDescription(Parameter parameter, String description) {
        if (parameter.getDescription() == null || parameter.getDescription().isBlank()) {
            parameter.setDescription(description);
        }
    }

    private Object buildRequestExample(String schemaName) {
        return switch (schemaName) {
            case "RegisterRequest" -> exampleMap(
                    "fullName", "Alice Customer",
                    "email", "alice.customer@example.com",
                    "phoneNumber", "0781234567",
                    "password", "Customer123!"
            );
            case "LoginRequest" -> exampleMap(
                    "email", "admin@utility.rw",
                    "password", "Admin123!"
            );
            case "VerifyEmailOtpRequest" -> exampleMap(
                    "email", "alice.customer@example.com",
                    "otpCode", "123456"
            );
            case "ResendVerificationOtpRequest" -> exampleMap(
                    "email", "alice.customer@example.com"
            );
            case "CreateStaffUserRequest" -> exampleMap(
                    "fullName", "Grace Operator",
                    "email", "grace.operator@example.com",
                    "phoneNumber", "0782345678",
                    "password", "Staff123!",
                    "role", "ROLE_OPERATOR"
            );
            case "CustomerRequest" -> exampleMap(
                    "fullName", "Jean Customer",
                    "nationalId", "1199080076543210",
                    "email", "jean.customer@example.com",
                    "phoneNumber", "0783456789",
                    "address", "Kigali, Gasabo, Kimironko",
                    "userId", null
            );
            case "CustomerProfileRequest" -> exampleMap(
                    "nationalId", "1199080076543210",
                    "address", "Kigali, Kicukiro, Niboye"
            );
            case "MeterRequest" -> exampleMap(
                    "meterNumber", "WTR-2026-0001",
                    "meterType", "WATER",
                    "installationDate", "2026-01-15",
                    "customerId", 1
            );
            case "MeterReadingRequest" -> exampleMap(
                    "meterId", 1,
                    "currentReading", new BigDecimal("250.00"),
                    "readingDate", "2026-03-05"
            );
            case "TariffRequest" -> exampleMap(
                    "name", "Standard Water Tariff 2026",
                    "meterType", "WATER",
                    "tariffType", "FLAT",
                    "ratePerUnit", new BigDecimal("120.50"),
                    "version", 1,
                    "effectiveFrom", "2026-01-01",
                    "effectiveTo", null,
                    "active", true
            );
            case "TariffTierRequest" -> exampleMap(
                    "minUnits", new BigDecimal("0"),
                    "maxUnits", new BigDecimal("50"),
                    "ratePerUnit", new BigDecimal("90.00")
            );
            case "FixedChargeRequest" -> exampleMap(
                    "meterType", "WATER",
                    "amount", new BigDecimal("5000.00"),
                    "version", 1,
                    "effectiveFrom", "2026-01-01",
                    "effectiveTo", null,
                    "active", true
            );
            case "TaxConfigRequest" -> exampleMap(
                    "name", "VAT",
                    "percentage", new BigDecimal("18.00"),
                    "active", true,
                    "effectiveFrom", "2026-01-01",
                    "effectiveTo", null
            );
            case "PenaltyConfigRequest" -> exampleMap(
                    "name", "Late Payment Penalty",
                    "penaltyType", "PERCENTAGE",
                    "amountOrPercentage", new BigDecimal("5.00"),
                    "gracePeriodDays", 5,
                    "active", true,
                    "effectiveFrom", "2026-01-01",
                    "effectiveTo", null
            );
            case "PaymentRequest" -> exampleMap(
                    "billReference", "BILL-2026-03-000001",
                    "amountPaid", new BigDecimal("1000.00"),
                    "paymentMethod", "MOBILE_MONEY",
                    "paymentDate", "2026-03-06"
            );
            default -> null;
        };
    }

    private Map<String, Object> exampleMap(Object... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], entries[index + 1]);
        }
        return values;
    }
}
