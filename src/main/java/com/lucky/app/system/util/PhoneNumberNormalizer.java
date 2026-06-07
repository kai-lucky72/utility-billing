package com.lucky.app.system.util;

/**
 * Normalizes Rwandan phone numbers to international E.164 format.
 *
 * <p>Input is expected to be a local 10-digit number starting with {@code 07}
 * (validated by the request DTOs). The Rwanda country code is prepended by
 * default so every stored number is unambiguously Rwandan, e.g.
 * {@code 0788123456} becomes {@code +250788123456}.
 */
public final class PhoneNumberNormalizer {

    private static final String RWANDA_PREFIX = "+25";
    private static final String LOCAL_PATTERN = "^07\\d{8}$";

    private PhoneNumberNormalizer() {
    }

    /**
     * Prepends the Rwanda country code to a local number.
     *
     * @param phoneNumber a local number such as {@code 0788123456}, or an
     *                    already-normalized number such as {@code +250788123456}
     * @return the number in {@code +250XXXXXXXXX} form, or the original value
     *         when it is null/already normalized
     */
    public static String toRwandaFormat(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String trimmed = phoneNumber.trim();
        if (trimmed.startsWith(RWANDA_PREFIX)) {
            return trimmed;
        }
        if (trimmed.matches(LOCAL_PATTERN)) {
            return RWANDA_PREFIX + trimmed;
        }
        return trimmed;
    }
}
