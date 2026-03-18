package org.simpmc.simppay.config.types.card;

import de.exlll.configlib.Configuration;
import org.simpmc.simppay.config.annotations.Folder;
import org.simpmc.simppay.handler.card.nencer.NencerCardConfig;

@Configuration
@Folder("card/thesieurecom")
public class ThesieureConfig implements NencerCardConfig {
    public String partnerId = "";
    public String partnerKey = "";

    @Override
    public String getPartnerId() {
        return partnerId;
    }

    @Override
    public String getPartnerKey() {
        return partnerKey;
    }
}
