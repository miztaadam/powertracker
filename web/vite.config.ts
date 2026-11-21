import { createHash } from 'node:crypto'
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

// Loaded into the generated service worker: reloads the windows the previous version controlled once a new version
// takes over (public/sw-reload-clients.js). Its URL carries a hash of its content, so sw.js changes whenever it does
// (the browser then installs the new worker) and no cache can hand back an older copy under the same URL.
const reloadClients = 'sw-reload-clients.js'
const reloadClientsHash = createHash('sha256').update(readFileSync(new URL(`./public/${reloadClients}`, import.meta.url))).digest('hex').slice(0, 12)

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
        // The app's main chunk passed the 2 MiB default in 1.16.1 (the shared domain is most of it). At the default,
        // vite-plugin-pwa stops `npm run build` with "error during build: ... Configure
        // "workbox.maximumFileSizeToCacheInBytes" to change the limit: the default value is 2 MiB. ... Assets exceeding
        // the limit: - assets/index-….js is 2.1 MB, and won't be precached." So the limit is 3 MiB.
        maximumFileSizeToCacheInBytes: 3 * 1024 * 1024,
        importScripts: [`${reloadClients}?v=${reloadClientsHash}`],
      },
    }),
  ],
  // The version and short commit id for the line at the bottom of Settings (plugins/buildInfo.ts).
  define: buildDefines(),
  build: { target: 'es2022' },
  preview: { headers: csp != null ? { 'Content-Security-Policy': csp } : {} },
})
