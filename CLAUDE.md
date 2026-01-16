# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SimpPay is a Minecraft plugin for automated Vietnamese payment processing (QR banking via PayOS/Web2M/Sepay and prepaid card recharging via TheSieuToc, Gachthe1s, Card2K, TheSieuRe, Doithe1s). Built for Paper 1.13-1.21.4+ servers using Java 8.

**Gradle structure:**
- `simppay-paper`: Main plugin implementation (contains all core code)
- `simppay-converter`: Data conversion utilities (minimal)

## Build Commands

```bash
./gradlew build                           # Build all, output: build/libs/SimpPay-Paper-<version>.jar
./gradlew clean build                     # Clean build
./gradlew :simppay-paper:shadowJar        # Create shaded JAR only
./gradlew :simppay-paper:sendRconCommand  # Build + reload plugin via RCON (localhost:25575)
```

## Core Architecture

### Service Pattern
All major functionality implements `IService` interface with `setup()` and `shutdown()` methods. Services are registered in `SPPlugin.onEnable()` and auto-register as event listeners if they implement `Listener`.

Access: `SPPlugin.getService(ServiceClass.class)`

**Key services:**
- `PaymentService` - Payment processing state, active payments map
- `DatabaseService` - ORMLite operations via DAOs
- `MilestoneService` - Cumulative recharge milestones with `MilestoneRepository` for persistence
- `CacheDataService` - In-memory cache with `LeaderboardEntry` tracking
- `StreakService` - Consecutive payment day tracking

### Handler System
Payment gateways use the `PaymentHandler` interface. Handlers are dynamically instantiated via reflection from enum types:
- `CardAPI` enum → Card handlers (`TSTHandler`, `GT1SHandler`, `Card2KHandler`, etc.)
- `BankAPI` enum → Bank handlers (`PayosHandler`, `W2MHandler`, `SepayHandler`)
- `CoinsAPI` enum → Points handlers (`PlayerPointsHandler`, `CoinsEngineHandler`, `DefaultCoinsHandler`)

### Configuration System
ConfigLib with YAML files managed by `ConfigManager`:
- Config classes in `config/types/` with `@Folder("subfolder")` for subdirectories
- Kebab-case YAML keys from camelCase field names (`NameFormatters.LOWER_KEBAB_CASE`)
- Custom serializers for Adventure API types (`Key`, `Sound`)
- Access: `ConfigManager.getInstance().getConfig(ConfigClass.class)`
- Register new configs in `ConfigManager.configClasses` list

**Key configs:**
- `StreakConfig` - Consecutive day rewards
- `MilestonesPlayerConfig` / `MilestonesServerConfig` - Human-readable milestone format with `MilestoneEntry`
- `SepayConfig` - Sepay banking integration

### UI System
Uses **InvUI** for chest GUIs (static `openMenu()` methods) and AnvilGUI for text input:
- `CardListView.openMenu(player)` → `CardPriceView.openMenu(player, cardType)` → AnvilGUI inputs
- `PaymentHistoryView.openMenu(player, playerName)` - Async data loading with `PagedGui`
- `StreakMenuView.openMenu(player)` - Streak progress display

Menu layouts configured via `DisplayItem` in `menus/*.yml` configs.

### Commands
CommandAPI (v11.1.0) in `CommandHandler.onEnable()`:
- `/napthe` - Card recharge GUI
- `/napthenhanh <card> <price> <serial> <pin>` - Quick recharge
- `/bank <amount>` - QR banking
- `/lichsunapthe` - Payment history
- `/streak` - Streak menu
- `/simppayadmin reload|lichsu|fakecard|fakebank` - Admin commands

Commands split into `commands/root/` and `commands/sub/`.

### Database
ORMLite ORM with HikariCP. Supports H2 (embedded) and MySQL/MariaDB.

**Entities:**
- `SPPlayer` - UUID/username mapping
- `CardPayment` / `BankingPayment` - Transaction records
- `PlayerData` - Cumulative totals by period
- `PlayerStreakPayment` - Streak tracking
- `MilestoneCompletion` - Prevents duplicate milestone rewards
- `LeaderboardCache` - Cached leaderboard data

### Event System
Custom events in payment lifecycle:
- `PaymentBankPromptEvent` → `PaymentSuccessEvent` / `PaymentFailedEvent`
- `PlayerMilestoneEvent` / `ServerMilestoneEvent` - Milestone completion

Listeners in `listener/internal/` handle: payment processing, cache updates, streak updates, milestone checks, database persistence.

### Async Scheduling
FoliaLib for Bukkit/Paper/Folia compatibility:
```java
SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> { });
SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> { });
```

### External Integrations
`HookManager` handles soft-dependencies. `FloodgateUtil` provides Bedrock player detection with `isBedrockPlayer()`, `getBedrockUsername()`, `getXboxUID()`, and `sendForm()` methods.

## Important Patterns

### Adding a New Payment Gateway
1. Create handler in `handler/[card|banking]/<gateway>/` implementing `PaymentHandler`
2. Add enum entry to `CardAPI` or `BankAPI` with handler class reference
3. Create config class in `config/types/[card|banking]/` with `@Folder` annotation
4. Register config in `ConfigManager.configClasses`

### Adding Config Files
1. Create config class in `config/types/` (use `@Folder("subfolder")` for subdirectories)
2. Add to `ConfigManager.configClasses` list
3. Access via `ConfigManager.getInstance().getConfig(YourConfig.class)`

### Message Localization
All messages in `MessageConfig.java` using MiniMessage format:
```java
MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
MessageUtil.sendMessage(player, messages.someMessage.replace("{placeholder}", value));
```

### Payment Processing Flow
1. Player initiates → `PaymentService.sendCard()` or `sendBank()`
2. Handler processes → returns `PaymentStatus.PENDING`
3. Async polling checks status
4. On success: `PaymentSuccessEvent` → awards points → updates cache → checks milestones → `StreakService.updateStreak()` → persists to database

### Milestone System
- `MilestoneRepository` tracks completed milestones to prevent duplicates
- `MilestoneListener` checks ALL uncompleted milestones on each payment (retroactive)
- Config uses `MilestoneEntry` with human-readable `name` field

## PlaceholderAPI Placeholders

```
%simppay_total%                    - Player's total recharge
%simppay_total_formatted%          - Formatted with đ suffix
%simppay_total_daily/weekly/monthly/yearly%  - Timed totals
%simppay_server_total%             - Server total
%simppay_streak_current%           - Current streak days
%simppay_streak_best%              - Best streak days
%simppay_leaderboard_<type>_<rank>_name%    - Leaderboard player name
%simppay_leaderboard_<type>_<rank>_amount%  - Leaderboard amount
```
Types: `all`, `daily`, `weekly`, `monthly`, `yearly`

## Testing

- No formal test suite
- RCON reload: `./gradlew :simppay-paper:sendRconCommand`
- Fake payments: `/simppayadmin fakecard`, `/simppayadmin fakebank`
