# Grit web

The iPhone-first, offline PWA version of Grit. It shares its coach and maths with the Android app
through the Kotlin domain in `../domain`, compiled to JavaScript and committed in `src/domain/generated/`.

## Develop

```sh
npm ci
npm run dev          # dev server, with the design-system showcase at `#/showcase`
npm test             # unit tests (Vitest)
npm run typecheck
npm run build        # production build in dist/
npm run e2e -- --trace off
```

The picker's timing checks ("picker speed") are their own Playwright project, `speed`, which runs after
everything else so nothing competes with them. To run them alone: `npm run e2e -- --trace off --project=speed --no-deps`.

After changing `../domain`, regenerate the committed JavaScript and check it is current:

```sh
./gradlew :domain:syncWebDomain
./gradlew :domain:checkWebDomain
```

## End-to-end tests

They run in Chromium with iPhone 13 emulation against `vite preview` (there is no WebKit in the
container). Real Safari checks are the manual list in the top-level `README.md`.
`vite preview` sends the Content-Security-Policy from `vercel.json`, so the tests run under the
policy the deployed app has.

Screenshots of every screen, dark and light, are written to `test-results/screens/` on a normal run.
To rewrite the committed ones in `docs/design/web/screens/`:

```sh
UPDATE_SCREENSHOTS=1 npm run e2e -- --trace off
```

## Deployment (Vercel)

The project is configured but not yet created or deployed.

- Root directory: `web/`
- Build command: `npm run build`
- Output directory: `dist`

For each update, bump `version` in `package.json` and `versionName`/`versionCode` in `../app/build.gradle.kts`
together. The Settings line shows the version and the deploy's short commit id (`VERCEL_GIT_COMMIT_SHA`).

`vercel.json` sets the Content-Security-Policy (`default-src 'self'`), long caching for hashed assets,
and `no-cache` for `index.html` and `sw.js`, so a new version is found at the next visit. Routing is by
URL hash, so no rewrites are needed. Passkeys belong to the hostname: set App lock up again after
moving to the final address.
