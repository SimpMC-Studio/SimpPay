package org.simpmc.simppay;

import com.github.retrooper.packetevents.PacketEvents;
import com.tcoded.folialib.FoliaLib;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
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
import org.simpmc.simppay.menu.PaymentHistoryView;
import org.simpmc.simppay.menu.ServerPaymentHistoryView;
import org.simpmc.simppay.menu.card.CardListView;
import org.simpmc.simppay.menu.card.CardPriceView;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.MilestoneService;
import org.simpmc.simppay.service.OrderIDService;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.service.cache.CacheDataService;

import java.io.File;
import java.sql.SQLException;
import java.util.Set;

public final class SPPlugin extends JavaPlugin {

    @Getter
    private static SPPlugin instance;
    @Getter
    private ConfigManager configManager;
    @Getter
    private FoliaLib foliaLib;
    private Database database;

    @Getter
    private CommandHandler commandHandler;

    @Getter
    private PaymentService paymentService;
    @Getter
    private CacheDataService cacheDataService;
    @Getter
    private MilestoneService milestoneService;
    @Getter
    private DatabaseService databaseService;
    private boolean dev = false;
    @Getter
    private ViewFrame viewFrame;
    @Getter
    private boolean floodgateEnabled;

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
            floodgateEnabled = true;
            getLogger().info("Enabled floodgate support");
        }
        // Thanks CHATGPT, qua met r
        OrderIDService.init(this);
        instance = this;
        foliaLib = new FoliaLib(this);
        // Plugin startup logic
        configManager = new ConfigManager(this);
        try {
            DatabaseSettings databaseConf = ConfigManager.getInstance().getConfig(DatabaseConfig.class);
            database = new Database(databaseConf);
        } catch (RuntimeException | SQLException e) {
            getLogger().warning("ChuyenXu failed to connect to database");
            this.getServer().getPluginManager().disablePlugin(this);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        cacheDataService = CacheDataService.getInstance();
        new HookManager(this);
        databaseService = new DatabaseService(database);
        paymentService = new PaymentService();
        milestoneService = new MilestoneService();
        registerListener();
        commandHandler.onEnable();
        registerInventoryFramework();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        PacketEvents.getAPI().terminate();
        this.getCacheDataService().clearAllCache();

        if (database != null) {
            database.close();
        }
        commandHandler.onDisable();
        OrderIDService.saveCurrent();
        instance = null;
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
