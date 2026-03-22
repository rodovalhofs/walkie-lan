import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      manifest: {
        name: "Walkie LAN Hybrid",
        short_name: "Walkie LAN",
        start_url: "/",
        display: "standalone",
        theme_color: "#112031",
        background_color: "#f3efe4",
        description: "Push-to-talk para LAN com entrada web no iPhone.",
        icons: [
          {
            src: "/icon-192.svg",
            sizes: "192x192",
            type: "image/svg+xml",
            purpose: "any"
          }
        ]
      }
    })
  ],
  server: {
    host: true,
    port: 5173
  }
});

