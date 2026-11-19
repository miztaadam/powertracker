import { readFileSync } from 'node:fs'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { buildDefines } from './plugins/buildInfo'
import { figureThumbnailsPlugin } from './plugins/figureThumbnails'

// The production headers live in vercel.json. `vite preview` sends the same Content-Security-Policy,
// so the end-to-end tests run under the policy the deployed app has.
type VercelHeaders = { headers: { source: string; headers: { key: string; value: string }[] }[] }
const vercel = JSON.parse(readFileSync(new URL('./vercel.json', import.meta.url), 'utf8')) as VercelHeaders
const csp = vercel.headers.find((h) => h.source === '/(.*)')?.headers.find((h) => h.key === 'Content-Security-Policy')?.value

export default defineConfig({
  plugins: [
    react(),
    figureThumbnailsPlugin(),
    VitePWA({
      registerType: 'prompt',
      manifest: false,
      includeAssets: ['icons/*.png', 'licenses/*.txt'],
      workbox: {
        // The mode switch sounds too (1.15), so they play offline from the first switch: Ogg Vorbis and its WAV fallback.
        globPatterns: ['**/*.{js,css,html,woff2,png,webmanifest,ogg,wav}'],
        navigateFallback: 'index.html',
        cleanupOutdatedCaches: true,
      },
    }),
  ],
  // The version and short commit id for the line at the bottom of Settings (plugins/buildInfo.ts).
  define: buildDefines(),
  build: { target: 'es2022' },
  preview: { headers: csp != null ? { 'Content-Security-Policy': csp } : {} },
})
