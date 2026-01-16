# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SimpPay is a multi-module Minecraft plugin for automated Vietnamese payment processing (QR banking via PayOS/Web2M and prepaid card recharging via TheSieuToc, Gachthe1s, Card2K, TheSieuRe, Doithe1s). Built for Paper 1.13-1.21.4+ servers using Java 8.

**Multi-module Gradle structure:**
- `simppay-api`: Core models, interfaces, and payment adapters (shared across modules)
- `simppay-paper`: Main plugin implementation with Paper API dependencies
- `simppay-converter`: Data conversion utilities (minimal dependencies)

## Build Commands

**Primary build command:**
```bash
./gradlew build
```
This builds all modules and produces `SimpPay-Paper-<version>.jar` in `build/libs/`.

**Module-specific builds:**
```bash
./gradlew :simppay-paper:build        # Build main plugin only
./gradlew :simppay-paper:shadowJar     # Create shaded JAR
```

**Development workflow:**
```bash
./gradlew :simppay-paper:sendRconCommand  # Build + reload plugin via RCON (localhost:25575)
```

**Clean build:**
```bash
./gradlew clean build
```

## Core Architecture

### Service Pattern
All major functionality implements `IService` interface:
```java
public interface IService {
    void setup();    // Called on plugin enable
    void shutdown(); // Called on plugin disable
}
```

Services are registered in `SPPlugin.onEnable()` via `registerServices()`. Services implementing `Listener` are auto-registered for events. Access services with `SPPlugin.getService(ServiceClass.class)`.

**Key services:**
- `PaymentService`: Manages payment processing state, maintains concurrent maps for active payments, banking sessions, QR codes
- `DatabaseService`: ORMLite database operations for payment records, player data, streaks
- `MilestoneService`: Tracks and rewards cumulative recharge milestones (daily/weekly/monthly/yearly)
- `OrderIDService`: Generates unique payment IDs
- `CacheDataService`: In-memory cache for player payment totals

### Handler System
Payment gateway integrations use the `PaymentHandler` interface. Each payment method has:
- **Card handlers** (`TSTHandler`, `GT1SHandler`, `Card2KHandler`, etc.) - Process prepaid card recharges
- **Bank handlers** (`PayosHandler`, `W2MHandler`) - Process QR banking transactions
- **Coins handlers** (`PlayerPointsHandler`, `CoinsEngineHandler`, `DefaultCoinsHandler`) - Award points/currency

`HandlerRegistry` dynamically instantiates handlers based on config settings (`CardConfig.cardApi`, `BankingConfig.bankApi`, `CoinsConfig.pointsProvider`). Handlers are loaded via reflection from enum types (`CardAPI`, `BankAPI`, `CoinsAPI`) that store handler class references.

### Configuration System
Uses ConfigLib with YAML files managed by `ConfigManager`:
- Configs implement Java classes with fields mapped to YAML keys (kebab-case naming via `NameFormatters.LOWER_KEBAB_CASE`)
- `@Folder("subfolder")` annotation places config YAMLs in subdirectories (e.g., `banking/payos-config.yml`)
- Custom serializers handle Adventure API types (`Key`, `Sound`)
- Config classes registered in `ConfigManager.configClasses` list
- Access via `ConfigManager.getInstance().getConfig(ConfigClass.class)`
- Reload all configs: `configManager.reloadAll()`

**Config structure:**
```
plugins/SimpPay/
├── main-config.yml              # Core settings
├── message-config.yml           # All player messages
├── database-config.yml          # Database connection
├── coins-config.yml             # Points provider selection
├── moc-nap-config.yml           # Player milestone rewards
├── moc-nap-server-config.yml    # Server-wide milestones
├── naplandau-config.yml         # First-time recharge bonuses
├── banking/
│   ├── banking-config.yml       # Banking gateway selection
│   ├── payos-config.yml         # PayOS API credentials
│   └── web2m-config.yml         # Web2M API credentials
├── card/
│   ├── card-config.yml          # Card gateway selection
│   ├── thesieutoc-config.yml    # TheSieuToc API config
│   ├── gachthe1s-config.yml
│   ├── card2k-config.yml
│   ├── thesieure-config.yml
│   └── doithe1s-config.yml
└── menus/
    ├── card-list-menu-config.yml
    ├── card-price-menu-config.yml
    ├── card-pin-menu-config.yml
    ├── card-serial-menu-config.yml
    ├── payment-history-menu-config.yml
    └── server-payment-history-menu-config.yml
```

### Commands
CommandAPI (v11.1.0) handles all commands, registered in `CommandHandler.onEnable()`:
- `/napthe` - Open card recharge GUI
- `/napthenhanh <card> <price> <serial> <pin>` - Quick recharge
- `/bank <amount>` - Generate QR code for banking
- `/lichsunapthe` - View player payment history
- `/simppayadmin reload` - Reload configs
- `/simppayadmin lichsu [player]` - View payment history (admin)
- `/manualcharge <player> <amount> <type>` - Manual credit (admin)

Commands are split into root commands (`commands/root/`) and subcommands (`commands/sub/`).

### Database
ORMLite ORM with HikariCP connection pooling. Supports H2 (embedded) and MySQL/MariaDB.

**Main entities:**
- `SPPlayer` - Player UUID/username mapping
- `CardPayment` / `BankingPayment` - Payment transaction records
- `PlayerData` - Cumulative payment totals by type/period
- `PlayerStreakPayment` - Milestone tracking data

Database DAOs accessed via `DatabaseService`.

### Event System
Custom Bukkit events fired during payment lifecycle:
- `PaymentBankPromptEvent` - Player initiates banking payment
- `PaymentSuccessEvent` - Payment completed successfully
- `PaymentFailedEvent` - Payment processing failed
- `PaymentQueueSuccessEvent` - Payment queued for processing
- `MilestoneCompleteEvent` - Base milestone completion event
- `PlayerMilestoneEvent` - Player reaches milestone
- `ServerMilestoneEvent` - Server reaches milestone

Listeners in `listener/internal/` handle payment state changes, cache updates, rewards.

### UI System
Uses `inventory-framework` (v3.5.0) for chest GUIs and AnvilGUI (v1.10.6) for text input:
- `ViewFrame` registers view classes in `SPPlugin.registerInventoryFramework()`
- Card selection flow: `CardListView` → `CardPriceView` → `CardSerialMenuConfig` → `CardPinMenuConfig` (AnvilGUI)
- Payment history: `PaymentHistoryView`, `ServerPaymentHistoryView`

Menu layouts configured in `menus/*.yml` using `DisplayItem` records.

### Async Scheduling
FoliaLib (v0.5.1) provides cross-compatible scheduling for Bukkit/Paper/Folia:
```java
SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
    // Async work
});
SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
    // Entity-specific task (Folia-safe)
});
```

### External Integrations
`HookManager` handles soft-dependencies:
- **PlaceholderAPI** - Exposes `%simppay_*%` placeholders for payment stats
- **Floodgate** - Bedrock player support (GeyserMC)
- **PlayerPoints** - Default points provider
- **CoinsEngine** - Alternative points provider

## Key Libraries

**Shaded dependencies** (relocated to `me.typical.lib.*`):
- ConfigLib 4.8.0 - YAML configuration
- CommandAPI 11.1.0 - Command framework
- FoliaLib 0.5.1 - Async scheduling
- inventory-framework 3.5.0 - GUI system
- PacketEvents 2.9.4 - Packet manipulation
- AnvilGUI 1.10.6-SNAPSHOT - Text input dialogs

**Runtime dependencies** (Paper PluginLoader):
- InvUI 1.49 - Additional GUI utilities
- OkHttp 5.0.0-alpha.12 - HTTP client for API calls
- ORMLite 6.1 - ORM framework
- HikariCP 6.3.0 - Connection pooling
- H2 2.3.232 - Embedded database

**CompileOnly dependencies:**
- Paper API 1.21.11-R0.1-SNAPSHOT
- PlaceholderAPI 2.11.6
- Floodgate API 2.2.4-SNAPSHOT
- PlayerPoints 3.3.0
- CoinsEngine 2.5.0

## Important Patterns

### Adding a New Payment Gateway
1. Create adapter class in `simppay-api/src/main/java/org/simpmc/simppay/data/[card|bank]/<gateway>/`
2. Implement `PaymentHandler` in `simppay-paper/src/main/java/org/simpmc/simppay/handler/[card|bank]/<gateway>/`
3. Add enum entry to `CardAPI` or `BankAPI` with handler class reference
4. Create config class in `config/types/[card|banking]/` with `@Folder` annotation
5. Register config in `ConfigManager.configClasses`

### Message Localization
All player-facing messages stored in `MessageConfig.java`. Use MiniMessage format:
```java
MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
player.sendMessage(MessageUtil.miniMessage(messages.someMessage, placeholder1, placeholder2));
```

### Adding Config Files
1. Create config class in `config/types/` (optionally use `@Folder("subfolder")`)
2. Add to `ConfigManager.configClasses` list
3. Access via `ConfigManager.getInstance().getConfig(YourConfig.class)`
4. Kebab-case YAML keys auto-generated from camelCase field names

### Payment Processing Flow
1. Player initiates payment (GUI or command)
2. `PaymentService.sendCard()` or `sendBank()` processes via appropriate handler
3. Handler returns `PaymentStatus.PENDING` → payment stored in concurrent map
4. Async polling checks payment status (gateway-specific implementation)
5. On completion: `PaymentSuccessEvent` → `SuccessHandlingListener` awards points → `CacheUpdaterListener` updates cache → `MilestoneListener` checks milestones
6. `SuccessDatabaseHandlingListener` persists to database

## Testing Notes

- No formal test suite present
- Manual testing workflow uses RCON reload via `sendRconCommand` Gradle task
- Test payment APIs provide sandbox credentials in gateway config files
- Fake payment commands for testing: `/simppayadmin fakecard`, `/simppayadmin fakebank`

## Dependencies Between Modules

- `simppay-paper` depends on `simppay-api` and `simppay-converter`
- `simppay-api` has no internal dependencies (only external compileOnly deps)
- Cross-module communication via interfaces defined in `simppay-api`
