# Card Input Flow Documentation

This document outlines the flow of card input in the system and provides an overview of the relevant components.

## Card Input Flow

1. **CardListView**:
    - Displays a list of available card options for the user to select.
    - The user selects a card type (e.g., `CardPrice`).

2. **CardPriceView**:
    - After selecting a card type, the user is prompted to choose the card price.
    - The selected price is stored and passed to the next step.

3. **CardSerialView**:
    - The user is prompted to input the card's serial number.
    - The serial number is validated and stored in a `CardDetail` object.

4. **CardPINView**:
    - The user is prompted to input the card's PIN.
    - The PIN is stored in the `CardDetail` object, and a `Payment` object is created using the card details and the
      player's UUID.

5. **Submit Card to Handler**:
    - The `Payment` object is submitted to the `PaymentService` for processing.
    - The system checks if the payment is already pending. If so, a message is sent to the player.
    - If not, the payment is processed, and the status (`FAILED`, `SUCCESS`, etc.) is returned.
    - Based on the status, appropriate messages and sound effects are sent to the player.