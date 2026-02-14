# SimpPay Documentation

Tài liệu hướng dẫn sử dụng SimpPay - Plugin thanh toán tự động cho Minecraft server.

## Development

### Prerequisites

- Node.js 18+
- npm

### Install dependencies

```bash
npm install
```

### Start dev server

```bash
npm run dev
```

Site sẽ chạy tại `http://localhost:4321/`

### Build for production

```bash
npm run build
```

Output sẽ được tạo trong thư mục `dist/`

### Preview production build

```bash
npm run preview
```

## Project Structure

```
docs/
├── src/
│   ├── content/
│   │   └── docs/           # All documentation pages (.mdx)
│   ├── styles/
│   │   └── custom.css      # Custom styles
│   └── content.config.ts   # Content collection config
├── public/
│   └── favicon.svg         # Site favicon
├── astro.config.mjs        # Astro + Starlight config
├── package.json
└── tsconfig.json
```

## Documentation Pages

### Getting Started (4 pages)
- Homepage (splash)
- Introduction
- Installation
- Quick Start

### Configuration (4 pages)
- Main Config
- Messages
- Database
- Coins & Rewards

### Payment Gateways (10 pages)
- Card Payment (6 pages: overview + 5 gateways)
- Banking Payment (4 pages: overview + 3 gateways)

### Features (5 pages)
- Milestones
- Streaks
- First Recharge
- Leaderboard
- Menus & GUI

### Commands (2 pages)
- Player Commands
- Admin Commands

### Placeholders (1 page)
- PlaceholderAPI

### Guides (4 pages)
- Adding Milestones
- Adding Streak Rewards
- Customizing Menus
- Adding Payment Gateway (Developer)

### API (1 page)
- Custom Events

**Total: 30 pages**

## Technologies

- [Astro](https://astro.build/) - Static site generator
- [Starlight](https://starlight.astro.build/) - Documentation theme
- [MiniMessage](https://docs.advntr.dev/minimessage/) - Text formatting examples

## Deployment

The site can be deployed to any static hosting service:

- **Vercel**: Connect GitHub repo and deploy automatically
- **Netlify**: Connect GitHub repo and deploy automatically
- **GitHub Pages**: Use GitHub Actions to build and deploy
- **Cloudflare Pages**: Connect GitHub repo and deploy automatically

Build command: `npm run build`
Output directory: `dist`

## License

Documentation is part of the SimpPay project.
