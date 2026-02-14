import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

export default defineConfig({
  site: 'https://simpmc-studio.github.io/SimpPay',
  integrations: [
    starlight({
      title: "SimpPay",
      description: "Hệ thống thanh toán tự động cho Minecraft server",
      defaultLocale: "root",
      locales: {
        root: {
          label: "Tiếng Việt",
          lang: "vi",
        },
      },
      social: {
        github: "https://github.com/SimpMC-Studio/SimpPay",
      },
      sidebar: [
        {
          label: "Bắt đầu",
          items: [
            { label: "Giới thiệu", link: "/getting-started/introduction/" },
            { label: "Cài đặt", link: "/getting-started/installation/" },
            { label: "Bắt đầu nhanh", link: "/getting-started/quick-start/" },
          ],
        },
        {
          label: "Cấu hình",
          items: [
            { label: "Cấu hình chính", link: "/configuration/main-config/" },
            { label: "Cơ sở dữ liệu", link: "/configuration/database/" },
            {
              label: "Xu & Phần thưởng",
              link: "/configuration/coins-rewards/",
            },
          ],
        },
        {
          label: "Cổng thanh toán",
          items: [
            {
              label: "Nạp thẻ cào",
              items: [
                { label: "Tổng quan", link: "/payment-gateways/card/" },
                {
                  label: "TheSieuToc",
                  link: "/payment-gateways/card/thesieutoc/",
                },
                { label: "Card2K", link: "/payment-gateways/card/card2k/" },
                {
                  label: "Gachthe1s",
                  link: "/payment-gateways/card/gachthe1s/",
                },
                {
                  label: "TheSieuRe",
                  link: "/payment-gateways/card/thesieure/",
                },
                { label: "Doithe1s", link: "/payment-gateways/card/doithe1s/" },
              ],
            },
            {
              label: "Thanh toán QR",
              items: [
                { label: "Tổng quan", link: "/payment-gateways/banking/" },
                { label: "PayOS", link: "/payment-gateways/banking/payos/" },
                { label: "Web2M", link: "/payment-gateways/banking/web2m/" },
                { label: "Sepay", link: "/payment-gateways/banking/sepay/" },
              ],
            },
          ],
        },
        {
          label: "Tính năng",
          items: [
            { label: "Hệ thống mốc nạp", link: "/features/milestones/" },
            { label: "Streak liên tiếp", link: "/features/streaks/" },
            { label: "Nạp lần đầu", link: "/features/naplandau/" },
            { label: "Bảng xếp hạng", link: "/features/leaderboard/" },
            { label: "Menu & GUI", link: "/features/menus/" },
          ],
        },
        {
          label: "Lệnh",
          items: [
            { label: "Lệnh người chơi", link: "/commands/player-commands/" },
            { label: "Lệnh admin", link: "/commands/admin-commands/" },
          ],
        },
        {
          label: "Placeholders",
          link: "/placeholders/",
        },
        {
          label: "Hướng dẫn",
          items: [
            { label: "Thêm mốc nạp mới", link: "/guides/adding-milestones/" },
            {
              label: "Thêm phần thưởng streak",
              link: "/guides/adding-streak-rewards/",
            },
            { label: "Tùy chỉnh menu", link: "/guides/customizing-menus/" },
            {
              label: "Thêm cổng thanh toán",
              link: "/guides/adding-payment-gateway/",
            },
          ],
        },
        {
          label: "API",
          items: [{ label: "Custom Events", link: "/api/events/" }],
        },
      ],
      customCss: ["./src/styles/custom.css"],
    }),
  ],
});
