package org.simpmc.simppay.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.simpmc.simppay.config.annotations.Folder;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.handler.data.CardAPI;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Folder("card")
public class CardConfig {
    @Comment("Dịch vụ gạch thẻ: GT1SCOM, CARD2K, THESIEURECOM, DOITHE1SVN")
    public CardAPI cardApi = CardAPI.GT1SCOM;

    @Comment("Các nhà mạng đang bật")
    public Map<CardType, Boolean> cardTypes = Map.of(
            CardType.VIETTEL, true,
            CardType.VINAPHONE, true,
            CardType.VIETNAMOBILE, true,
            CardType.MOBIFONE, true,
            CardType.GATE, true,
            CardType.GARENA, true,
            CardType.VCOIN, true,
            CardType.ZING, true
    );

    public List<CardType> getEnabledCardTypes() {
        return cardTypes.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
