package org.simpmc.simppay;

import com.github.retrooper.packetevents.PacketEvents;
import com.tcoded.folialib.FoliaLib;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.api.DatabaseSettings;
import org.simpmc.simppay.commands.CommandHandler;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.DatabaseConfig;
import org.simpmc.simppay.database.Database;
import org.simpmc.simppay.hook.HookManager;
import org.simpmc.simppay.listener.internal.cache.CacheUpdaterListener;
import org.simpmc.simppay.listener.internal.milestone.MilestoneListener;
import org.simpmc.simppay.listener.internal.payment.PaymentHandlingListener;
import org.simpmc.simppay.listener.internal.player.BankPromptListener;
import org.simpmc.simppay.listener.internal.player.NaplandauListener;
import org.simpmc.simppay.listener.internal.player.SuccessHandlingListener;
import org.simpmc.simppay.listener.internal.player.database.SuccessDatabaseHandlingListener;
import org.simpmc.simppay.service.*;
import org.simpmc.simppay.service.cache.CacheDataService;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public final class SPPlugin extends JavaPlugin {

    @Getter
    private static SPPlugin instance;
    private final List<IService> services = Collections.synchronizedList(new ArrayList<>());
    @Getter
    private ConfigManager configManager;
    @Getter
    private FoliaLib foliaLib;
    @Getter
    private CommandHandler commandHandler;
    private boolean dev = false;
    @Getter
    private boolean floodgateEnabled;

    public static @NotNull <T extends IService> T getService(Class<T> clazz) {
        for (var service : instance.getServices())
            if (clazz.isAssignableFrom(service.getClass())) {
                return clazz.cast(service);
            }

        instance.getLogger().severe("Service " + clazz.getName() + " not instantiated. Did you forget to create it?");
        throw new RuntimeException("Service " + clazz.getName() + " not instantiated?");
    }


    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        commandHandler = new CommandHandler(this);
        commandHandler.onLoad();
    }

    // TODO: A fking mess, please fix
    @Override
    public void onEnable() {
        // Reset config
        PacketEvents.getAPI().init();
        registerMetrics();
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateEnabled = org.simpmc.simppay.util.FloodgateUtil.initialize();
            if (floodgateEnabled) {
                getLogger().info("Floodgate support enabled successfully");
            } else {
                getLogger().warning("Floodgate detected but initialization failed");
            }
        }
        // Thanks CHATGPT, qua met r
        instance = this;
        foliaLib = new FoliaLib(this);
        // Plugin startup logic
        configManager = new ConfigManager(this);

        Database database = null;
        try {
            DatabaseSettings databaseConf = ConfigManager.getInstance().getConfig(DatabaseConfig.class);
            database = new Database(databaseConf);
        } catch (RuntimeException | SQLException e) {
            getLogger().warning("SimpPay failed to connect to database");
            this.getServer().getPluginManager().disablePlugin(this);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        services.add(new OrderIDService());
        services.add(new CacheDataService());
        services.add(new DatabaseService(database));
        services.add(new PaymentService());
        services.add(new MilestoneService());

        registerServices();

        new HookManager(this);
        registerListener();
        commandHandler.onEnable();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        PacketEvents.getAPI().terminate();
        for (var service : services) {
            try {
                service.shutdown();
            } catch (Exception e) {
                getLogger().severe("Failed to shutdown service: " + service.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
        if (commandHandler.enabled) {
            commandHandler.onDisable();
        }
        instance = null;
    }

    private void registerServices() {
        for (var service : services) {
            service.setup();
            getLogger().info(service.getClass().getSimpleName() + " service successfully enabled!");

            if (service instanceof Listener listener) {
                getServer().getPluginManager().registerEvents(listener, instance);
                getLogger().info(service.getClass().getSimpleName() + " is now listening to events.");
            }
        }
    }

    private void registerListener() {
        Set<Class<? extends Listener>> listeners = Set.of(
                PaymentHandlingListener.class,
                BankPromptListener.class,
                SuccessHandlingListener.class,
                SuccessDatabaseHandlingListener.class,
                CacheUpdaterListener.class,
                MilestoneListener.class,
                NaplandauListener.class
        );

        for (Class<? extends Listener> listener : listeners) {
            try {
                listener.getConstructor(SPPlugin.class).newInstance(this);
            } catch (Exception e) {
                getLogger().warning("Failed to register listener: " + listener.getSimpleName());
                e.printStackTrace();
            }
        }
    }

    public Collection<IService> getServices() {
        return services;
    }

    private void registerMetrics() {
        Metrics metrics = new Metrics(this, 25693);
        // check competitors stuff
        File dotManFolder = new File(getDataFolder().getParent(), "DotMan");
        File hmtopupFolder = new File(getDataFolder().getParent(), "HMTopUp");
        metrics.addCustomChart(new Metrics.SimplePie("had_dotman", () -> String.valueOf(dotManFolder.exists())));
        metrics.addCustomChart(new Metrics.SimplePie("had_hmtopup", () -> String.valueOf(hmtopupFolder.exists())));
    }

}
