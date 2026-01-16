package org.simpmc.simppay.data.card;

import java.util.Arrays;

public enum CardType {
    VIETTEL("Viettel"),
    VINAPHONE("Vinaphone"),
    VIETNAMOBILE("Vietnamobile"),
    MOBIFONE("Mobifone"),
    GATE("Gate"),
    GARENA("Garena"),
    VCOIN("Vcoin"),
    ZING("Zing");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Converts a lowercase string to the corresponding CardType.
     *
     * @param type a lowercase string matching the enum name (e.g., "viettel").
     * @return the matching CardType
     * @throws IllegalArgumentException if no matching CardType is found
     */
    public static CardType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type string cannot be null");
        }
        return Arrays.stream(CardType.values())
                .filter(cardType -> cardType.name().equalsIgnoreCase(type.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown card type: " + type));
    }

    /**
     * Returns an array of all card type display names.
     *
     * @return array of display names
     */
    public static String[] getAllCardTypes() {
        return Arrays.stream(CardType.values())
                .map(CardType::toString)
                .toArray(String[]::new);
    }

    /**
     * Returns the display name of this card type.
     */
    @Override
    public String toString() {

        return displayName;
    }
}
