package org.simpmc.simppay.convert;

import java.util.function.Supplier;

public enum ConverterRegistry {
    THESIEUTOC("thesieutoc", "Thesieutoc", ThesieutocConverter::new);
    // Future entries: DOTMAN("dotman", "DotMan", DotManConverter::new), etc.

    public final String commandName;
    public final String displayName;
    public final Supplier<PluginConverter> factory;

    ConverterRegistry(String commandName, String displayName, Supplier<PluginConverter> factory) {
        this.commandName = commandName;
        this.displayName = displayName;
        this.factory = factory;
    }
}
