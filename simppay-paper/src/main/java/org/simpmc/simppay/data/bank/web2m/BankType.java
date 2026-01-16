package org.simpmc.simppay.data.bank.web2m;

public enum BankType {
    VCB("970436", "historyapivcbv3", false),
    BIDV("970418", "historyapibidvv3", false),
    BIDV_OPENAPI("970418", "historyapiopenbidvv3", true),
    MBBANK_OPENAPI("970422", "historyapiopenmbv3", true),
    ACB("970416", "historyapiacbv3", false),
    TECHCOMBANK("970407", "historyapitcbv3", false),
    MBBBANK_LSGD("970422", "historyapimbv3", false),
    MBBANK_NOTI("970422", "historyapimbnotiv3", false),
    TPBANK("970423", "historyapitpbv3", false);

    public final String bin;
    public final String web2mPath;
    public final boolean isOneParam;

    BankType(String bin, String web2mPath, boolean isOneParam) {
        this.bin = bin;
        this.web2mPath = web2mPath;
        this.isOneParam = isOneParam;
    }
    // bin, web2m path, is one param ?

    public static String getValues() {
        StringBuilder values = new StringBuilder();
        for (BankType bank : BankType.values()) {
            if (!values.isEmpty()) {
                values.append(", ");
            }
            values.append(bank.name());
        }
        return values.toString();
    }


}
