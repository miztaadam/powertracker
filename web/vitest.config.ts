import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { buildDefines } from './plugins/buildInfo'
import { figureThumbnailsPlugin } from './plugins/figureThumbnails'

export default defineConfig({
  plugins: [react(), figureThumbnailsPlugin()],
  // A fixed build id, so no test depends on the real commit.
  define: buildDefines({ GRIT_BUILD_ID: 'a1b2c3d' }),
  // vite-plugin-pwa's virtual module exists only in a real build.
  resolve: { alias: { 'virtual:pwa-register': fileURLToPath(new URL('./src/test/pwaRegister.ts', import.meta.url)) } },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
