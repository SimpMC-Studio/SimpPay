package org.simpmc.simppay.util;

/**
 * Utility for converting reward commands into human-readable descriptions.
 */
public class CommandDescriptionUtil {

    /**
     * Parses a command string into a human-readable description.
     * Example: "give %player_name% diamond 5" -> "5x Diamond"
     */
    public static String parseCommandToDescription(String command) {
        if (command.contains("give")) {
            String[] parts = command.split(" ");
            if (parts.length >= 4) {
                String item = parts[2];
                String amount = parts[3];
                return amount + "x " + formatItemName(item);
            }
        }
        return command;
    }

    /**
     * Formats a material name to be more readable.
     * Example: "gold_ingot" -> "Gold Ingot"
     */
    public static String formatItemName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return result.toString();
    }
}
