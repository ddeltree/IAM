import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// O backend autentica pelo cookie `uid`, mas o CORS dele é `anyHost()` — que o
// navegador não aceita junto com credenciais. Passando por este proxy, o app
// fala com a própria origem (/api/...) e o cookie viaja naturalmente até o :7000.
const proxy = {
  '/api': {
    target: 'http://localhost:7000',
    changeOrigin: true,
    rewrite: (caminho: string) => caminho.replace(/^\/api/, ''),
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: { proxy },
  preview: { proxy },
})
