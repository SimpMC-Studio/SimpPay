package org.simpmc.simppay.config.types.card;

import de.exlll.configlib.Configuration;
import org.simpmc.simppay.config.annotations.Folder;
import org.simpmc.simppay.handler.card.nencer.NencerCardConfig;

@Configuration
@Folder("card/doithe1svn")
public class Doithe1sConfig implements NencerCardConfig {
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
