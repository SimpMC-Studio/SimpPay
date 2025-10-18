# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SimpPay is a multi-module Bukkit/Paper plugin that provides automated QR payment and prepaid mobile card recharge solutions for Vietnamese Minecraft servers. It integrates with payment gateways like Thesieutoc and PayOS, and supports both Java Edition and Bedrock (via Floodgate).

## Build Commands

The project uses Gradle as the build system.

### Build the Plugin
```bash
./gradlew build
```
This compiles all modules and generates the shaded JAR file at `./build/libs/SimpPay-Paper-{version}.jar`. The build automatically creates a fat JAR with all dependencies relocated under `me.typical.lib.*`.

### Build Specific Module
```bash
./gradlew :simppay-paper:build     # Build Paper plugin only
./gradlew :simppay-api:build       # Build API module only
./gradlew :simppay-converter:build # Build converter module only
```

### Tasks
```bash
./gradlew tasks              # List all available tasks
./gradlew shadowJar          # Build the shaded JAR (usually run by build)
./gradlew sendRconCommand    # Deploy to local test server via RCON
```

### Troubleshooting Build Issues

- **Compilation errors**: Ensure Java 8+ is installed (`java -version`)
- **Dependency issues**: Run `./gradlew clean` then `./gradlew build`
- **RCON issues**: The `sendRconCommand` task requires server credentials in `simppay-paper/build.gradle`

## Project Structure

### Multi-Module Layout

```
SimpPay (root)
├── simppay-api/          # Shared APIs and models
│   ├── api/              # Core interfaces (DatabaseSettings, etc.)
│   ├── data/             # Payment data types, adapters
│   ├── handler/          # Payment handler interface
│   ├── model/            # Data models (Payment, PaymentResult, etc.)
│   └── util/             # Utility classes
│
├── simppay-paper/        # Main Bukkit/Paper plugin implementation
│   ├── commands/         # CommandAPI-based command implementations
│   ├── config/           # ConfigLib YAML configuration system
│   ├── database/         # ORM entities and Database class
│   ├── event/            # Custom events
│   ├── handler/          # Payment handler implementations (card, bank)
│   ├── listener/         # Bukkit event listeners
│   ├── menu/             # Inventory Framework UI menus
│   ├── service/          # Core service classes (PaymentService, etc.)
│   ├── util/             # Utilities (messages, etc.)
│   └── SPPlugin.java     # Main plugin class
│
├── simppay-converter/    # Data conversion utilities
│   └── convert/          # Conversion logic
│
└── gradle files          # Gradle build configuration

```

### Module Dependencies

- **simppay-api**: Shared foundation, no Bukkit dependencies
- **simppay-paper**: Depends on simppay-api, Paper API, and all external libraries
- **simppay-converter**: Depends on simppay-api

## Core Architecture

### Plugin Lifecycle (SPPlugin.java)

The main plugin class manages the entire lifecycle:

1. **onLoad()**: Initializes PacketEvents and CommandHandler
2. **onEnable()**:
   - Sets up ConfigManager to load all YAML configurations
   - Creates Database instance with configured connection pool
   - Registers core services (OrderIDService, CacheDataService, DatabaseService, PaymentService, MilestoneService)
   - Calls `setup()` on all services
   - Registers event listeners
   - Initializes Inventory Framework for menus
3. **onDisable()**: Properly shuts down all services and closes database connections

### Service Pattern

All major components implement `IService` interface with `setup()` and `shutdown()` methods:

- **OrderIDService**: Manages unique order ID generation
- **CacheDataService**: Caches player recharge data for fast placeholder lookups
- **DatabaseService**: Wraps the Database instance
- **PaymentService**: Orchestrates card and bank payment processing
- **MilestoneService**: Handles recharge milestone tracking and rewards

Services are accessed via `SPPlugin.getService(ServiceClass.class)`.

### Configuration System (ConfigManager)

Uses ConfigLib with YAML files organized by feature:

- **Root configs**: main-config.yml, message-config.yml, coins-config.yml, database-config.yml
- **Card configs**: `card/thesieutoc-config.yml`, card provider configs
- **Banking configs**: `banking/payos-config.yml`, bank provider configs
- **Menu configs**: `menus/` subdirectory for UI configurations
- **Milestone configs**: moc-nap-config.yml (player), moc-nap-server-config.yml (server-wide)

All config classes in `config/types/` are auto-loaded. Use `@Folder` annotation to organize into subfolders. New configs are registered in the `configClasses` list in ConfigManager.

### Payment Processing Flow

1. **Card/Bank Payment Request**: User initiates via `/napthe` command
2. **PaymentService**: Routes to appropriate handler (CardHandler or BankHandler)
3. **Handler Implementation**:
   - ThesieutocHandler for card payments
   - PayosHandler or Web2mHandler for banking
   - Each extends CardAdapter or BankAdapter respectively
4. **Payment Status**: Returns PENDING for async polling, or SUCCESS/FAILED immediately
5. **Event Emission**: On completion, fires PaymentSuccessEvent
6. **Success Handling**: Listeners process coins, milestones, and notifications

### Database (ORM-based)

Uses ORM Lite with HikariCP connection pooling. Supports MySQL and H2:

Key entities in `database/entities/`:
- **BankingPayment**: Bank transfer records
- **CardPayment**: Card recharge records
- **SPPlayer**: Player account info
- **PlayerData**: Flexible EAV-style player attributes
- **PlayerStreakPayment**: Recharge streak tracking

Database class handles connection setup, entity initialization, and provides DAOs via getters.

### Event System

Custom events in `event/` package (e.g., PaymentSuccessEvent) are fired by handlers and listened to by payment processing listeners. This enables loose coupling between payment providers and business logic.

### Command System (CommandAPI)

Commands are implemented using dev.jorel:commandapi library:

- Root commands in `commands/root/` (e.g., `/napthe`)
- Subcommands in `commands/sub/` organized by feature
- Admin commands in `commands/root/admin/` and `commands/sub/admin/`
- CommandHandler manages registration during plugin lifecycle

### UI Menus (Inventory Framework)

Uses me.devnatan.inventoryframework for GUIs:

- CardListView, CardPriceView for card recharges
- PaymentHistoryView, ServerPaymentHistoryView for history browsing
- Custom anvil-based menus for PIN/Serial input
- All views registered with ViewFrame in onEnable()

### Floodgate Integration

Bedrock player support via Floodgate API. Plugin detects Floodgate presence on startup and provides form-based UI alternatives for Bedrock players.

## Key Dependencies

- **Paper API 1.21.7**: Bukkit/Paper server implementation
- **ConfigLib**: YAML configuration management with auto-reload
- **CommandAPI 10.1.2**: Advanced command handling with auto-complete
- **FoliaLib 0.5.1**: Cross-compatible task scheduling (Bukkit/Paper/Folia)
- **InventoryFramework 3.5.0**: Menu/GUI framework
- **ORM Lite 6.1**: Database ORM with HikariCP
- **PaymentHandler libraries**: Thesieutoc, PayOS, Web2m adapters
- **PacketEvents 2.9.4**: Packet-level event handling
- **Lombok**: Code generation for boilerplate

All dependencies are shaded and relocated under `me.typical.lib.*` in the final JAR.

## Java Conventions

- **Java 8 target**: Code targets Java 8 for broader server compatibility
- **Lombok annotations**: Heavy use of @Getter, @Data, @Slf4j for boilerplate reduction
- **Exception handling**: Custom exceptions in `exception/` package
- **Async tasks**: Use FoliaLib scheduler via `SPPlugin.getFoliaLib().getScheduler()` for compatibility with Folia multithreading

## Configuration Pattern

When adding new features that require configuration:

1. Create config class in `config/types/` extending appropriate base
2. Use ConfigLib annotations for YAML field mapping
3. Add to `configClasses` list in ConfigManager.java
4. Use `@Folder` annotation if organizing into subdirectory
5. Custom serializers in `config/serializers/` handle complex types (Key, Sound, etc.)
6. Access via `ConfigManager.getInstance().getConfig(YourConfigClass.class)`

## Testing

No dedicated test suite is present. Testing is manual or via server deployment.

## Common Issues & Solutions

- **Database connection failures**: Check database config and ensure MySQL/H2 is running
- **Config not loading**: Verify config class is added to `configClasses` list in ConfigManager
- **Payment handler not found**: Ensure payment adapter config is present and correct API keys are configured
- **Floodgate not working**: Install Floodgate plugin separately; SimpPay auto-detects it

## Notes

- Plugin requires PlaceholderAPI and PlayerPoints to function
- Placeholders support leaderboard plugins like ajLeaderboards and Topper
- Metrics tracking is enabled (bstats ID 25693)
- The codebase marks the onEnable() method as "a fucking mess" and suggests refactoring (TODO comment at line 71 of SPPlugin.java)
