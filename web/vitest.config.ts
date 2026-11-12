import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { figureThumbnailsPlugin } from './plugins/figureThumbnails'

export default defineConfig({
  plugins: [react(), figureThumbnailsPlugin()],
  // vite-plugin-pwa's virtual module exists only in a real build.
  resolve: { alias: { 'virtual:pwa-register': fileURLToPath(new URL('./src/test/pwaRegister.ts', import.meta.url)) } },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
