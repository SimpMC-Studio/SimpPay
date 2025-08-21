
Simp Pay [![Discord](https://img.shields.io/discord/1353293624238145626.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.typicalsmc.me/discord) ![Supported server version](https://img.shields.io/badge/minecraft-1.13%20--_1.21.4-green) [![wakatime](https://wakatime.com/badge/user/a9b10cc4-84e7-4a4d-9952-09cd1d1d7750/project/35b700a7-bd03-4fef-96c8-4ea2c0988f46.svg)](https://wakatime.com/badge/user/a9b10cc4-84e7-4a4d-9952-09cd1d1d7750/project/35b700a7-bd03-4fef-96c8-4ea2c0988f46)
===========
Language: [Vietnamese](README_VN.md), **[English](README.md)** <br><br>
Automated QR payment and prepaid mobile card recharge solution for Vietnamese Minecraft Servers <br> <br>
**Supported payment gateways:** [thesieutoc](https://thesieutoc.net/), [payos](https://payos.vn/)

![Bstats](https://bstats.org/signatures/bukkit/SimpPay.svg)

Current Features
===========

- Automatic card recharge
- Almost everything can be customized via config
- Rewards based on recharge milestones
- Quick recharge commands with auto-complete
- Recharge milestones by day/week/month/year
- Interface for Bedrock / GeyserMC
- View player recharge history

Usage Guide
===========

**Plugin Installation:**

- The plugin requires
  [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/)
  and [PlaceholderAPI
](https://www.spigotmc.org/resources/placeholderapi.6245/) to function
- Download the plugin [here](https://github.com/SimpMC-Studio/SimpPay/releases/), and place it in the `plugins` folder
- You need to install floodgate to use the card recharge interface for Bedrock players, download
  it [here](https://geysermc.org/download?project=floodgate)

**Command List:**

| Command                     | Function                                 | Permission                |
|-----------------------------|------------------------------------------|---------------------------|
| /napthe                     | Open recharge card menu                  | simppay.napthe            |
| /simppayadmin lichsu        | View server-wide recharge history        | simppay.admin.viewhistory |
| /simppayadmin lichsu <name> | View recharge history of specific player | simppay.admin.viewhistory |
| /lichsunapthe               | View recharge history                    | simppay.lichsunapthe      |
| /bank <amount>              | Bank deposit via QR code                 | simppay.banking           |

**Placeholders:**

Placeholders can be used to display top rechargers using
[ajLeaderboards](https://www.spigotmc.org/resources/ajleaderboards.85548/)
or [topper](https://www.spigotmc.org/resources/topper.101325/)

| Placeholder                      | Function                                            | Notes |
|----------------------------------|-----------------------------------------------------|-------|
| %simppay_total%                  | Returns total recharge of that player               |       |
| %simppay_total_formatted%        | Returns player's recharge amount in xxx.xxxđ format |       |
| %simppay_server_total%           | Returns total server recharge                       |       |
| %simppay_server_total_formatted% | Returns server recharge amount in xxx.xxxđ format   |       |
| %simppay_bank_total_formatted%   | Returns bank recharge amount in xxx.xxxđ format     |       |
| %simppay_card_total_formatted%   | Returns card recharge amount in xxx.xxxđ format     |       |
| %simppay_end_promo%              | Returns End Promo dd/MM/yyyy HH:mm format           |       |

**Plugin Configuration:**

The directory structure of `./plugins/SimpPay` is as follows

```
SimpPay
│   coins-config.yml 
│   database-config.yml
│   last_id.txt
│   main-config.yml
│   message-config.yml
│   moc-nap-config.yml
│   moc-nap-server-config.yml
│   naplandau-config.yml
│
├───banking
│   │   banking-config.yml
│   │
│   └───payos
│           payos-config.yml
│
├───card
│   │   card-config.yml
│   │
│   └───thesieutoc
│           thesieutoc-config.yml
│
└───menus
        card-list-menu-config.yml
        card-pin-menu-config.yml
        card-price-menu-config.yml
        card-serial-menu-config.yml
        payment-history-menu-config.yml
        server-payment-history-menu-config.yml
```

- You can configure the interface in the files within the `menus` directory
- To add API Keys for corresponding services, add them in the files within the `banking` and `card` directories
- General plugin settings are in `main-config.yml`, messages are in `message-config.yml`
- Configure cumulative recharge milestones in `moc-nap-config.yml` and `moc-nap-server-config.yml`
- Configure first-time recharge commands in `naplandau-config.yml`

[![Powered by DartNode](https://dartnode.com/branding/DN-Open-Source-sm.png)](https://dartnode.com "Powered by DartNode - Free VPS for Open Source")
