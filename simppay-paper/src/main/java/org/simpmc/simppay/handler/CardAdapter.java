package org.simpmc.simppay.handler;

import org.simpmc.simppay.data.card.CardType;

public interface CardAdapter {
    String adaptCardType(CardType cardType);
}
