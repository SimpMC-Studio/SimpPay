package org.simpmc.simppay.exception;

public class CardProcessException extends RuntimeException {
    public CardProcessException(String message) {
        super("An error occurred while trying to fetch the data returned by the card during sending" + message);
    }
}
