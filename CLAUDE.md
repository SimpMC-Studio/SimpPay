# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SimpPay is a Minecraft plugin for automated Vietnamese payment processing (QR banking via PayOS/Web2M/Sepay and prepaid card recharging via TheSieuToc, Gachthe1s, Card2K, TheSieuRe, Doithe1s). Built for Paper 1.13-1.21.4+ servers using Java 8.

**Gradle structure:**
- `simppay-paper`: Main plugin implementation (contains all core code)
- `simppay-converter`: Data conversion utilities (minimal)

## Build Commands

```bash
./gradlew build                    # Build all, output: build/libs/SimpPay-<version>.jar
./gradlew clean build              # Clean build
./gradlew :simppay-paper:shadowJar # Create shaded JAR only
```

**Output location:** `build/libs/SimpPay-<version>.jar` (can override with `OUTPUT_DIR` env var)

**Dependencies:** All shaded dependencies are relocated under `me.typical.lib.*`:
- ConfigLib → `me.typical.lib.configlib`
- CommandAPI → `me.typical.lib.commandapi`
- FoliaLib → `me.typical.lib.folialib`

**Paper Plugin Loader:** Uses `SimpPayLoader` (implements `PluginLoader`) to download dependencies at runtime via Maven repositories. This ensures compatibility with Paper 1.21+ plugin loading system. Dependencies are fetched from:
- Maven Central (Google mirror)
- XenonDevs (InvUI)
- SpaceIO (AnvilGUI snapshots)

**Runtime-loaded libraries:**
- InvUI 1.49
- OkHttp 5.0.0-alpha.12
- ConfigLib 4.8.0
- ORMLite 6.1
- HikariCP 6.3.0
- H2 Database 2.3.232
- Lombok 1.18.34
- Commons Codec 1.18.0
- AnvilGUI 2.0.3-SNAPSHOT

## Core Architecture

### Service Pattern
All major functionality implements `IService` interface with `setup()` and `shutdown()` methods. Services are registered in `SPPlugin.onEnable()` and auto-register as event listeners if they implement `Listener`.

**Registration order (important for dependency resolution):**
1. `OrderIDService` - Sequential payment ID generation
2. `BankCacheService` - Async VietQR bank data cache (fetched on startup)
3. `CacheDataService` - In-memory leaderboard cache with 1-minute TTL
4. `DatabaseService` - ORMLite DAOs and sub-services
5. `PaymentService` - Active payment tracking and handler routing
6. `MilestoneService` - Milestone tracking with BossBar display
7. `WebhookService` - HTTP server for Sepay webhook callbacks (port 8080 by default)

Access: `SPPlugin.getService(ServiceClass.class)`

### Handler System
Payment gateways use the `PaymentHandler` interface. Handlers are dynamically instantiated via reflection from enum types in `HandlerRegistry`:
- `CardAPI` enum → Card handlers (`TSTHandler`, `GT1SHandler`, `Card2KHandler`, `TSRHandler`, `DT1SHandler`)
- `BankAPI` enum → Bank handlers (`PayosHandler`, `W2MHandler`, `SepayHandler`)
- `CoinsAPI` enum → Points handlers (`PlayerPointsHandler`, `CoinsEngineHandler`, `DefaultCoinsHandler`)

Each enum specifies its handler class reference. Handlers are loaded during `setup()` and on config reload.

### Configuration System
ConfigLib with YAML files managed by `ConfigManager`:
- Config classes in `config/types/` with `@Folder("subfolder")` for subdirectories
- Kebab-case YAML keys from camelCase field names (`NameFormatters.LOWER_KEBAB_CASE`)
- Custom serializers for Adventure API types (`Key`, `Sound`)
- Access: `ConfigManager.getInstance().getConfig(ConfigClass.class)`
- Register new configs in `ConfigManager.configClasses` list

**Key configs:**
- `MainConfig` - Core plugin settings (debug mode, locale)
- `MessageConfig` - MiniMessage-formatted player messages
- `DatabaseConfig` - H2/MySQL/MariaDB connection settings
- `StreakConfig` - Consecutive day rewards configuration
- `MilestonesPlayerConfig` / `MilestonesServerConfig` - Milestone rewards with `MilestoneEntry`
- `NaplandauConfig` - First-time recharge rewards
- Banking configs: `PayosConfig`, `Web2mConfig`, `SepayConfig`
- Card configs: `ThesieutocConfig`, `Card2kConfig`, `Gachthe1sConfig`, etc.

### UI System
Uses **InvUI** (v1.49) for chest GUIs (static `openMenu()` methods) and AnvilGUI for text input:

**Menu flow:**
```
CardListView.openMenu(player)
  → Shows enabled card types
  → Clicks open CardPriceView.openMenu(player, cardType)
    → Shows price options (10k, 20k, 50k, etc.)
    → Clicks open CardSerialInput (AnvilGUI)
      → User enters serial number
      → Opens CardPinInput (AnvilGUI)
        → User enters PIN
        → Initiates payment via PaymentService
```

**Other menus:**
- `PaymentHistoryView.openMenu(player, playerName)` - Async-loaded paginated transaction history with `PagedGui`
- `StreakMenuView.openMenu(player)` - Streak progress display with milestone rewards

Menu layouts configured via `DisplayItem` in `menus/*.yml` configs.

### Commands
CommandAPI (v11.1.0) managed by `CommandHandler`:
- `/napthe` - Open card recharge GUI (permission: `simppay.napthe`)
- `/napthenhanh <card> <price> <serial> <pin>` - Quick recharge without GUI (permission: `simppay.napthenhanh`)
- `/bank <amount>` - QR banking recharge (permission: `simppay.banking`)
- `/lichsunapthe [player]` - Payment history (permission: `simppay.lichsunapthe`)
- `/streak` - Streak menu (permission: `simppay.streak`)
- `/simppayadmin reload` - Reload all configs (permission: `simppay.admin.reload`)
- `/simppayadmin lichsu [player]` - View recharge history (permission: `simppay.admin.viewhistory`)
- `/simppayadmin fakecard <player> <amount>` - Simulate card payment for testing
- `/simppayadmin fakebank <player> <amount>` - Simulate bank payment for testing

Commands split into `commands/root/` and `commands/sub/`.

### Database
ORMLite ORM with HikariCP connection pooling. Supports H2 (embedded) and MySQL/MariaDB.

**Entities:**
- `SPPlayer` - UUID/username mapping (main player record)
- `CardPayment` / `BankingPayment` - Transaction records with price, serial, PIN (card only), status
- `PlayerData` - Cumulative totals by period (DAILY, WEEKLY, MONTHLY, YEARLY, ALL)
- `PlayerStreakPayment` - Consecutive day streak tracking with best streak
- `MilestoneCompletion` - Audit trail to prevent duplicate milestone rewards
- `LeaderboardCache` - Cached leaderboard data for performance

**DAO Services (accessed via DatabaseService):**
- `PlayerService` - Player lookups, creation, UUID/name resolution
- `PaymentLogService` - Transaction queries with batch optimizations
- `PlayerDataService` - Period-based total tracking and updates
- `StreakService` - Streak calculation, updates, and reward checks

**Database migrations:** No formal migration system. Uses `TableUtils.createTableIfNotExists()` for schema management.

### Event System
Custom events fired during payment lifecycle:

**Payment events:**
- `PaymentBankPromptEvent` - Display QR code/payment link to player
- `PaymentQueueSuccessEvent` - Payment queued for status polling
- `PaymentSuccessEvent` - Payment confirmed successful (triggers rewards)
- `PaymentFailedEvent` - Payment failed or rejected

**Milestone events:**
- `PlayerMilestoneEvent` - Player milestone completed
- `ServerMilestoneEvent` - Server-wide milestone completed
- `MilestoneCompleteEvent` - Generic milestone completion

**Webhook events:**
- `SepayWebhookReceivedEvent` - Sepay webhook received and validated (fired async)

**Listener execution order (important):**
1. `PaymentHandlingListener` - Initiates async polling for payment status
2. `SuccessHandlingListener` - Awards points/coins on success
3. `CacheUpdaterListener` - Updates leaderboard cache synchronously
4. `MilestoneListener` - Checks ALL uncompleted milestones (retroactive)
5. `SuccessDatabaseHandlingListener` - Updates streak via `StreakService.updateStreak()`
6. Database persistence in `DatabaseService`

### Async Scheduling
FoliaLib for Bukkit/Paper/Folia compatibility. Provides cross-compatible task scheduling:

```java
// Async tasks (safe for blocking operations like HTTP requests)
SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
    // Background work
});

// Entity-specific tasks (required for entity operations on Folia)
SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
    // Player-specific operations
});

// Repeated async tasks (used for payment polling)
SPPlugin.getInstance().getFoliaLib().getScheduler().runTimerAsync(task -> {
    // Repeated background work
}, delayTicks, periodTicks);

// Next tick (main thread on Spigot, global region on Folia)
SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
    // Sync work
});
```

### External Integrations
**HookManager** manages soft-dependencies:
- PlaceholderAPI - Placeholder expansion for leaderboards and stats
- PlayerPoints - Primary points provider
- CoinsEngine - Alternative points provider
- Floodgate - Bedrock player support

**FloodgateUtil** provides Bedrock player detection:
- `isBedrockPlayer(UUID)` - Check if player is from Bedrock
- `getBedrockUsername(UUID)` - Get Bedrock display name
- `getXboxUID(UUID)` - Get Xbox UID
- `sendForm(Player, Form)` - Send Bedrock forms to players

### Sepay Integration
**Sepay** is the newest banking gateway with webhook support for instant payment confirmation:

**QR Code Generation:**
- Uses Sepay's QR code API instead of local VietQR generation
- API endpoint: `https://my.sepay.vn/userapi/transactions/create-qr`
- Returns base64-encoded QR code image for display

**WebhookService** provides HTTP webhook endpoint for Sepay real-time payment notifications:
- Creates HTTP server on configurable port (default: 8080)
- Endpoint path: `/webhook/sepay` (configurable in `sepay-config.yml`)
- Validates incoming requests with `Authorization: Apikey YOUR_KEY` header
- Fires `SepayWebhookReceivedEvent` for processing by `SepayWebhookListener`
- Uses virtual threads (Java 21+) for high-performance webhook handling
- Only processes incoming transfers (ignores outgoing)

**Configuration:**
```yaml
webhook-port: 8080
webhook-path: "/webhook/sepay"
webhook-api-key: "YOUR_WEBHOOK_API_KEY_HERE"  # Must match Sepay dashboard config
```

**Security:** Webhook endpoint validates API key before processing. Invalid keys return 403 Forbidden.

**Payment Flow:**
1. Player initiates `/bank <amount>`
2. `SepayHandler` calls Sepay QR API to generate payment QR
3. QR displayed to player via `PaymentBankPromptEvent`
4. Player scans and pays
5. Sepay sends webhook to plugin's HTTP server
6. `SepayWebhookListener` validates payment and fires `PaymentSuccessEvent`

### Bank Data Cache
**BankCacheService** fetches and caches Vietnamese bank metadata from VietQR API:
- Async fetch on plugin startup (non-blocking)
- Stores bank short names, BINs, and display names
- Case-insensitive lookup by bank short name
- Used by Sepay handler for bank name resolution
- Lifetime cache (no TTL, cleared on shutdown)
- Access via `SPPlugin.getService(BankCacheService.class).getBankByName(name)`

## Important Patterns

### Adding a New Payment Gateway
1. Create handler in `handler/[card|banking]/<gateway>/` implementing `PaymentHandler`
2. Add enum entry to `CardAPI` or `BankAPI` with handler class reference:
   ```java
   NEW_GATEWAY("New Gateway", NewGatewayHandler.class)
   ```
3. Create config class in `config/types/[card|banking]/` with `@Folder` annotation:
   ```java
   @Folder("card") // or "banking"
   public class NewGatewayConfig { ... }
   ```
4. Register config in `ConfigManager.configClasses` list
5. Implement required handler methods: `send()`, `check()`, `getAPI()`, `getConfig()`

### Adding Config Files
1. Create config class in `config/types/` (use `@Folder("subfolder")` for subdirectories)
2. Add to `ConfigManager.configClasses` list:
   ```java
   private final List<Class<?>> configClasses = List.of(
       MainConfig.class,
       MessageConfig.class,
       YourNewConfig.class  // Add here
   );
   ```
3. Access via `ConfigManager.getInstance().getConfig(YourNewConfig.class)`

**Config reload:** `/simppayadmin reload` reloads all configs AND re-initializes `HandlerRegistry` (handlers get new config instances).

### Message Localization
All messages in `MessageConfig.java` using MiniMessage format:
```java
MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
MessageUtil.sendMessage(player, messages.someMessage.replace("{placeholder}", value));
```

**Debug logging:**
```java
MessageUtil.debug("Debug info"); // Only shows if MainConfig.debug = true
```

### Payment Processing Flow
1. Player initiates → `PaymentService.sendCard()` or `sendBank()`
2. Handler validates and processes → returns `PaymentStatus.PENDING`
3. `PaymentQueueSuccessEvent` fired → `PaymentHandlingListener.addTaskChecking()` starts async polling
4. Polling checks payment status every N seconds (configurable timeout)
5. On success:
   - `PaymentSuccessEvent` fired
   - `SuccessHandlingListener` awards points/coins via `CoinsHandler`
   - `CacheUpdaterListener` updates leaderboard cache synchronously
   - `MilestoneListener` checks ALL uncompleted milestones (retroactive checking)
   - `StreakService.updateStreak()` updates consecutive day streak
   - Database persistence via `DatabaseService`

### Milestone System
**Architecture:**
- `MilestoneRepository` tracks completed milestones in database to prevent duplicates
- `MilestoneListener` checks ALL uncompleted milestones on each payment (retroactive)
- Supports 5 milestone types: `ALL`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`
- Config uses `MilestoneEntry` with human-readable `name` field for tracking

**Retroactive checking:**
When a player makes ANY payment, the system checks if their cumulative total NOW meets ANY uncompleted milestone threshold. This allows players to claim milestones they may have missed.

**BossBar display:**
`MilestoneService` creates dynamic BossBars showing progress toward next milestone. Progress bars update on every payment.

### Streak System
**Logic (StreakService):**
- First payment on a new day = streak starts at 1
- Payment on consecutive day = increment streak
- Gap > 1 day = reset to 1
- Multiple payments same day = no change
- Best streak tracked separately (never decreases)

**Reward tiers:**
Configurable in `StreakConfig` with milestone-based rewards:
```yaml
milestones:
  3:
    commands: ["give {player} diamond 3"]
    message: "3-day streak reward!"
  7:
    commands: ["give {player} emerald 1"]
    message: "7-day streak reward!"
```

**Date calculation:** Uses `ZoneId.systemDefault()` for local timezone. Compares LocalDate to determine consecutive days.

### Cache System
**CacheDataService optimization:**
- Batch query approach reduces database queries (5 queries → 2 queries)
- Synchronous cache updates on `PaymentSuccessEvent` (no queue delay)
- Leaderboard data cached with 1-minute TTL
- Lazy loading for PlaceholderAPI placeholder requests
- `LeaderboardEntry` stores: UUID, name, amount for quick access

**Performance:** Cache invalidation on every payment success ensures fresh data without polling overhead.

## PlaceholderAPI Placeholders

**Player stats:**
```
%simppay_total%                    - Player's total recharge (raw number)
%simppay_total_formatted%          - Formatted with xxx.xxxđ suffix
%simppay_total_daily%              - Daily total (raw)
%simppay_total_daily_formatted%    - Daily total formatted
%simppay_total_weekly%             - Weekly total (raw)
%simppay_total_weekly_formatted%   - Weekly total formatted
%simppay_total_monthly%            - Monthly total (raw)
%simppay_total_monthly_formatted%  - Monthly total formatted
%simppay_total_yearly%             - Yearly total (raw)
%simppay_total_yearly_formatted%   - Yearly total formatted
%simppay_card_total%               - Card recharge total (raw)
%simppay_card_total_formatted%     - Card total formatted
%simppay_bank_total%               - Bank recharge total (raw)
%simppay_bank_total_formatted%     - Bank total formatted
```

**Server stats:**
```
%simppay_server_total%             - Server total recharge (raw)
%simppay_server_total_formatted%   - Server total formatted
```

**Streak info:**
```
%simppay_streak_current%           - Current streak days
%simppay_streak_best%              - Best streak days
```

**Leaderboard (requires cache):**
```
%simppay_leaderboard_<type>_<rank>_name%    - Player name at rank
%simppay_leaderboard_<type>_<rank>_amount%  - Amount at rank
```
Types: `all`, `daily`, `weekly`, `monthly`, `yearly`
Example: `%simppay_leaderboard_all_1_name%` = #1 player name

**Promo:**
```
%simppay_end_promo%                - End promo date (dd/MM/yyyy HH:mm format)
```

## Testing & Debugging

**No formal test suite.** Use these approaches:

**Fake payments (testing):**
```
/simppayadmin fakecard <player> <amount>   # Simulate card payment
/simppayadmin fakebank <player> <amount>   # Simulate bank payment
```

**Config reload:**
```
/simppayadmin reload  # Reloads all configs + handler registry
```

**Debug logging:**
Enable in `main-config.yml`:
```yaml
debug: true  # Shows debug messages via MessageUtil.debug()
```

**Payment lifecycle debugging:**
1. Enable debug mode
2. Initiate payment
3. Check console for:
   - Handler selection and config loading
   - Payment status polling logs
   - Event firing sequence
   - Cache updates
   - Milestone checks
   - Streak calculations

**Common issues:**
- Missing API keys → Check `[card|banking]/<gateway>-config.yml`
- Database connection → Check `database-config.yml` credentials
- Handler not loading → Verify enum entry and class path in `HandlerRegistry`
- Milestones not triggering → Check `MilestoneCompletion` table for duplicates
