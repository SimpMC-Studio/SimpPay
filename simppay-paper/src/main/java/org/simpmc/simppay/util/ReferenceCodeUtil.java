package org.simpmc.simppay.util;

import java.security.SecureRandom;

/**
 * Utility class for generating unique reference codes for payment matching.
 */
public class ReferenceCodeUtil {
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a random 10-character alphanumeric reference code.
     * 
     * @return A 10-character uppercase alphanumeric string
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a full reference code with the given prefix.
     * 
     * @param prefix The prefix to prepend (e.g., "smc123")
     * @return The full reference code (prefix + 10-char code)
     */
    public static String generateWithPrefix(String prefix) {
        return prefix + generate();
    }
}
