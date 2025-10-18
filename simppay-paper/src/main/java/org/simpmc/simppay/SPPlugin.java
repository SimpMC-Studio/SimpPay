package org.simpmc.simppay;

import com.github.retrooper.packetevents.PacketEvents;
import com.tcoded.folialib.FoliaLib;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.api.DatabaseSettings;
import org.simpmc.simppay.commands.CommandHandler;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.DatabaseConfig;
import org.simpmc.simppay.database.Database;
import org.simpmc.simppay.hook.HookManager;
import org.simpmc.simppay.menu.PaymentHistoryView;
import org.simpmc.simppay.menu.ServerPaymentHistoryView;
import org.simpmc.simppay.menu.card.CardListView;
import org.simpmc.simppay.menu.card.CardPriceView;
import org.simpmc.simppay.service.*;
import org.simpmc.simppay.service.cache.CacheDataService;
import org.simpmc.simppay.listener.internal.cache.CacheUpdaterListener;
import org.simpmc.simppay.listener.internal.milestone.MilestoneListener;
import org.simpmc.simppay.listener.internal.payment.PaymentHandlingListener;
import org.simpmc.simppay.listener.internal.player.BankPromptListener;
import org.simpmc.simppay.listener.internal.player.NaplandauListener;
import org.simpmc.simppay.listener.internal.player.SuccessHandlingListener;
import org.simpmc.simppay.listener.internal.player.database.SuccessDatabaseHandlingListener;
import org.simpmc.simppay.migration.MigrationService;

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
    private ViewFrame viewFrame;
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

    @Override
    public void onEnable() {
        getLogger().info("Starting SimpPay plugin initialization...");

        // Initialize core systems
        instance = this;
        foliaLib = new FoliaLib(this);

        // Phase 1: Load configurations
        configManager = new ConfigManager(this);
        getLogger().info("✓ Configuration loaded");

        // Phase 2: Initialize packet handling
        initializePacketEvents();

        // Phase 3: Setup metrics
        registerMetrics();
        getLogger().info("✓ Metrics initialized");

        // Phase 4: Detect external plugins
        detectFloodgate();

        // Phase 5: Initialize database
        Database database = initializeDatabase();
        if (database == null) {
            return;
        }

        // Phase 6: Register core services
        registerCoreServices(database);
        registerServices();
        getLogger().info("✓ Core services registered");

        // Phase 7: Setup external plugin hooks
        setupHooks();

        // Phase 8: Setup command system
        commandHandler.onEnable();
        getLogger().info("✓ Command system initialized");

        // Phase 9: Setup UI framework
        setupInventoryFramework();

        getLogger().info("✓ SimpPay plugin fully initialized!");
    }

    /**
     * Initialize PacketEvents API
     */
    private void initializePacketEvents() {
        PacketEvents.getAPI().init();
        getLogger().info("✓ PacketEvents initialized");
    }

    /**
     * Detect if Floodgate is installed for Bedrock player support
     */
    private void detectFloodgate() {
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateEnabled = true;
            getLogger().info("✓ Floodgate integration enabled (Bedrock support)");
        }
    }

    /**
     * Initialize database connection and create tables
     *
     * @return Database instance or null if initialization failed
     */
    private Database initializeDatabase() {
        try {
            DatabaseSettings databaseConf = ConfigManager.getInstance().getConfig(DatabaseConfig.class);
            Database database = new Database(databaseConf);
            getLogger().info("✓ Database connection established");
            return database;
        } catch (RuntimeException | SQLException e) {
            getLogger().severe("Failed to connect to database!");
            getLogger().severe("SimpPay will now disable...");
            e.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            return null;
        }
    }

    /**
     * Register all core services that manage plugin functionality
     *
     * @param database The initialized database instance
     */
    private void registerCoreServices(Database database) {
        // Core services - order matters!
        services.add(new OrderIDService());
        services.add(new CacheDataService());
        DatabaseService databaseService = new DatabaseService(database);
        services.add(databaseService);
        services.add(new PaymentService());
        services.add(new MilestoneService(databaseService));

        // Event listeners (now registered as services)
        services.add(new CacheUpdaterListener(this));
        services.add(new PaymentHandlingListener(this));
        services.add(new BankPromptListener(this));
        services.add(new SuccessHandlingListener(this));
        services.add(new SuccessDatabaseHandlingListener(this));
        services.add(new MilestoneListener(this));
        services.add(new NaplandauListener(this));
    }

    /**
     * Setup external plugin integrations (hooks)
     */
    private void setupHooks() {
        new HookManager(this);
        getLogger().info("✓ Plugin hooks initialized");
    }

    /**
     * Setup inventory framework for menu UIs
     */
    private void setupInventoryFramework() {
        registerInventoryFramework();
        getLogger().info("✓ Inventory framework configured");
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

    /**
     * Register all services by calling their setup() method
     */
    private void registerServices() {
        for (var service : services) {
            service.setup();
            getLogger().info(service.getClass().getSimpleName() + " initialized successfully!");
        }
    }

    public Collection<IService> getServices() {
        return services;
    }

    private void registerInventoryFramework() {
        viewFrame = ViewFrame.create(this)
                .with(
                        new CardListView(),
                        new CardPriceView(),
                        new PaymentHistoryView(),
                        new ServerPaymentHistoryView()
                )
                .disableMetrics()
                .register();
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
