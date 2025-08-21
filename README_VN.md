Simp Pay [![Discord](https://img.shields.io/discord/1353293624238145626.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.typicalsmc.me/discord) ![Supported server version](https://img.shields.io/badge/minecraft-1.13%20--_1.21.4-green)
===========
Ngôn ngữ: **[Vietnamese](README_VN.md)**, [English](README.md)

Giải pháp thanh toán QR và thẻ cào tự động cho Server Minecraft Việt Nam

**Các loại cổng nạp đang hỗ trợ:** [thesieutoc](https://thesieutoc.net/), [payos](https://payos.vn/)

![Bstats](https://bstats.org/signatures/bukkit/SimpPay.svg)

Tính năng hiện có
===========

- Nạp thẻ tự động
- Gần như tất cả đều có thể tùy chỉnh qua config
- Phần thưởng theo mốc nạp
- Lệnh nạp nhanh với auto-complete
- Mốc nạp theo ngày/tuần/tháng/năm
- Giao diện dành cho Bedrock / GeyserMC
- Xem lại lịch sử nạp của người chơi

Hướng dẫn sử dụng
===========

**Cài đặt plugin:**

- Plugin cần
  có [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/)
  và [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) để hoạt động
- Tải plugin [tại đây](https://github.com/SimpMC-Studio/SimpPay/releases/), và để vào thư mục `plugins`
- Cần cài thêm floodgate để sử dụng giao diện nạp thẻ cho người chơi Bedrock, tải
  tại [đây](https://geysermc.org/download?project=floodgate)

**Danh sách lệnh:**

| Lệnh                        | Chức năng                               | Permission                |
|-----------------------------|-----------------------------------------|---------------------------|
| /napthe                     | Mở menu nạp thẻ                         | simppay.napthe            |
| /simppayadmin lichsu        | Xem lịch sử nạp toàn server             | simppay.admin.viewhistory |
| /simppayadmin lichsu <name> | Xem lịch sử nạp của người chơi chỉ định | simppay.admin.viewhistory |
| /lichsunapthe               | Xem lịch sử nạp                         | simppay.lichsunapthe      |
| /bank <số tiền>             | Nạp ngân hàng qua mã QR                 | simppay.banking           |

**Placeholder:**

Placeholder có thể sử dụng để hiển thị top nạp bằng cách sử
dụng [ajLeaderboards](https://www.spigotmc.org/resources/ajleaderboards.85548/)
hoặc [topper](https://www.spigotmc.org/resources/topper.101325/)

| Placeholder                      | Chức năng                                          | Ghi chú |
|----------------------------------|----------------------------------------------------|---------|
| %simppay_total%                  | Trả về tổng nạp của người chơi đó                  |         |
| %simppay_total_formatted%        | Trả về số tiền nạp của người chơi đó dạng xxx.xxxđ |         |
| %simppay_server_total%           | Trả về tổng nạp toàn server                        |         |
| %simppay_server_total_formatted% | Trả về số tiền nạp toàn server dạng xxx.xxxđ       |         |
| %simppay_bank_total_formatted%   | Trả về số tiền nạp ngân hàng dạng xxx.xxxđ         |         |
| %simppay_card_total_formatted%   | Trả về số tiền nạp thẻ dạng xxx.xxxđ               |         |
| %simppay_end_promo%              | Trả về thời gian kết thúc khuyến mãi dạng dd/MM/yyyy HH:mm |         |

**Config plugin:**

Cấu trúc thư mục `./plugins/SimpPay` như sau

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

- Bạn có thể config giao diện tại các file trong thư mục `menus`
- Để thêm API Key cho các dịch vụ tương ứng, hãy thêm tại các file trong thư mục `banking` và `card`
- Cài đặt chung của plugin được đặt tại `config.yml`, các message đặt tại `messages.yml`
- Cài đặt mốc nạp tích luỹ tại `moc-nap-config.yml` và `moc-nap-server-config.yml`
- Cài đặt lệnh nạp lần đầu tại `naplandau-config.yml`

[![Powered by DartNode](https://dartnode.com/branding/DN-Open-Source-sm.png)](https://dartnode.com "Powered by DartNode - Free VPS for Open Source")
