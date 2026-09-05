import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// O console roda na 5174 e fala com o backend dele na 7001 — o classroom
// continua na 5173/7000, e os dois não se cruzam. O proxy mantém tudo na
// mesma origem, como no outro app.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:7001',
        changeOrigin: true,
        rewrite: (caminho: string) => caminho.replace(/^\/api/, ''),
      },
    },
  },
})
