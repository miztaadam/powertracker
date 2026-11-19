#!/usr/bin/env node
// Renders the two mode switch sounds from the approved prototype's own Web Audio synthesis
// (docs/design/motion/mode-switch-sound.html), once, and writes them for both apps:
//   app/src/main/res/raw/grit_hammer_drop.ogg, grit_pump.ogg   (the same bytes as the web's .ogg)
//   web/public/sounds/grit_hammer_drop.ogg,    grit_pump.ogg
//   web/public/sounds/grit_hammer_drop.wav,    grit_pump.wav    (the web's fallback where Ogg Vorbis can't play)
//
// The page renders each sound once in headless Chromium with an OfflineAudioContext at 48 kHz, limits it to its
// ceiling and fades the tail (window.__sound.render(kind)); this script takes those samples as they are.
// With ffmpeg on the PATH the .ogg files are OGG Vorbis (mono, 48 kHz). The .wav files are 16-bit PCM, as the page's
// own export writes them. Without ffmpeg there is no .ogg: Android gets the WAV instead, and the old .ogg is removed
// from res/raw and from the web's folder, so no folder holds two grit_* files of one name (in res/raw that would be
// two resources of one name, and the build would fail).
//
// Every file is measured before anything is written (decoded again for the .ogg): length 1.9 s (drop) and 2.0 s
// (pump), peak at most -1 dBFS. If any is off, nothing is written.
//
// Usage, from the repository root:  node tools/render-mode-switch-sounds.js
// It uses web/'s Playwright and the pre-installed Chromium; it never downloads a browser.

'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..');
const REFERENCE = path.join(ROOT, 'docs/design/motion/mode-switch-sound.html');
const OUT_DIRS = [path.join(ROOT, 'app/src/main/res/raw'), path.join(ROOT, 'web/public/sounds')];
const SOUNDS = [
  { kind: 'drop', name: 'grit_hammer_drop', seconds: 1.9 },
  { kind: 'pump', name: 'grit_pump', seconds: 2.0 },
];
const PEAK_LIMIT_DB = -1;
// q7 keeps the drop's decoded peak about 0.2 dB under the limit (q5 left 0.02 dB); higher ones are tried in turn if not
const VORBIS_QUALITY = [7, 8, 9];
const CHROMIUM = '/opt/pw-browsers/chromium'; // as in web/playwright.config.ts

const { chromium } = createRequire(path.join(ROOT, 'web/package.json'))('playwright');

function hasFfmpeg() {
  try { execFileSync('ffmpeg', ['-hide_banner', '-version'], { stdio: 'ignore' }); return true; } catch { return false; }
}

function wavBytes(samples, sr) { // the page's wavBytes: 16-bit mono PCM
  const n = samples.length, buf = Buffer.alloc(44 + n * 2);
  buf.write('RIFF', 0); buf.writeUInt32LE(36 + n * 2, 4); buf.write('WAVE', 8); buf.write('fmt ', 12);
  buf.writeUInt32LE(16, 16); buf.writeUInt16LE(1, 20); buf.writeUInt16LE(1, 22); buf.writeUInt32LE(sr, 24);
  buf.writeUInt32LE(sr * 2, 28); buf.writeUInt16LE(2, 32); buf.writeUInt16LE(16, 34); buf.write('data', 36); buf.writeUInt32LE(n * 2, 40);
  for (let i = 0; i < n; i++) buf.writeInt16LE(Math.round(Math.max(-1, Math.min(1, samples[i])) * 32767), 44 + i * 2);
  return buf;
}

// Decodes a file to 32-bit float mono and measures it the way the page does.
function measureFile(file, sr) {
  const raw = execFileSync('ffmpeg', ['-v', 'error', '-i', file, '-f', 'f32le', '-ac', '1', '-ar', String(sr), '-'], { maxBuffer: 64 << 20 });
  const x = new Float32Array(raw.buffer, raw.byteOffset, raw.length / 4);
  let peak = 0; for (const v of x) peak = Math.max(peak, Math.abs(v));
  return { seconds: x.length / sr, peak };
}

function measureWav(buf, sr) {
  const n = (buf.length - 44) / 2; let peak = 0;
  for (let i = 0; i < n; i++) peak = Math.max(peak, Math.abs(buf.readInt16LE(44 + i * 2) / 32768));
  return { seconds: n / sr, peak };
}

const dB = (v) => 20 * Math.log10(Math.max(v, 1e-9));

async function renderAll() {
  const browser = await chromium.launch(fs.existsSync(CHROMIUM) ? { executablePath: CHROMIUM } : {});
  try {
    const page = await browser.newPage();
    await page.route(/fonts\.(googleapis|gstatic)\.com/, (r) => r.abort()); // the page's web fonts: not needed to render sound
    await page.goto('file://' + REFERENCE, { waitUntil: 'domcontentloaded' });
    await page.waitForFunction(() => window.__sound && typeof window.__sound.render === 'function');
    const out = {};
    for (const s of SOUNDS) out[s.kind] = await page.evaluate((kind) => window.__sound.render(kind), s.kind);
    out.rawPeak = await page.evaluate(() => ({ ...window.__sound.rawPeak }));
    return out;
  } finally {
    await browser.close();
  }
}

async function main() {
  const ogg = hasFfmpeg();
  const rendered = await renderAll();
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'mode-switch-sounds-'));
  const [RAW, WEB] = OUT_DIRS;
  const writes = []; // [dir, file, bytes], made only once every file has been checked
  const removes = []; // [dir, file]
  let failed = false;
  function check(file, measured, seconds, bytes, extra) {
    const lengthOk = Math.abs(measured.seconds - seconds) < 0.005;
    const peakOk = dB(measured.peak) <= PEAK_LIMIT_DB;
    if (!lengthOk || !peakOk) failed = true;
    console.log(`${file}: ${measured.seconds.toFixed(3)} s (want ${seconds}) ${lengthOk ? 'ok' : 'WRONG'}, ` +
      `peak ${dB(measured.peak).toFixed(2)} dBFS (limit ${PEAK_LIMIT_DB}) ${peakOk ? 'ok' : 'TOO LOUD'}, ` +
      `${(bytes.length / 1024).toFixed(1)} kB${extra}`);
  }
  try {
    for (const s of SOUNDS) {
      const r = rendered[s.kind];
      console.log(`${s.name}: rendered peak before the limiter ${dB(rendered.rawPeak[s.kind]).toFixed(2)} dBFS, ${r.sr} Hz`);
      const wav = wavBytes(r.data, r.sr);
      check(`${s.name}.wav`, measureWav(wav, r.sr), s.seconds, wav, '');
      writes.push([WEB, `${s.name}.wav`, wav]);
      if (ogg) {
        const src = path.join(tmp, s.name + '.wav'); fs.writeFileSync(src, wav);
        let bytes = null, measured = null, quality = null;
        for (const q of VORBIS_QUALITY) {
          const dst = path.join(tmp, `${s.name}.q${q}.ogg`);
          execFileSync('ffmpeg', ['-v', 'error', '-y', '-i', src, '-map_metadata', '-1', '-c:a', 'libvorbis', '-q:a', String(q),
            '-ac', '1', '-ar', String(r.sr), '-fflags', '+bitexact', '-flags:a', '+bitexact', dst]);
          measured = measureFile(dst, r.sr); bytes = fs.readFileSync(dst); quality = q;
          if (dB(measured.peak) <= PEAK_LIMIT_DB) break;
        }
        check(`${s.name}.ogg`, measured, s.seconds, bytes, `, Vorbis q${quality}`);
        writes.push([RAW, `${s.name}.ogg`, bytes], [WEB, `${s.name}.ogg`, bytes]);
        removes.push([RAW, `${s.name}.wav`]);
      } else {
        writes.push([RAW, `${s.name}.wav`, wav]);
        removes.push([RAW, `${s.name}.ogg`], [WEB, `${s.name}.ogg`]);
      }
    }
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
  if (failed) { console.error('A sound is off the reference: see above. Nothing was written.'); process.exit(1); }
  for (const [dir, file] of removes) {
    const at = path.join(dir, file);
    if (fs.existsSync(at)) { fs.rmSync(at); console.log(`removed ${path.relative(ROOT, at)}`); }
  }
  for (const [dir, file, bytes] of writes) {
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(path.join(dir, file), bytes);
    console.log(`wrote ${path.relative(ROOT, path.join(dir, file))}`);
  }
  if (!ogg) console.log('No ffmpeg: WAV only. The web plays the WAV wherever it cannot find or play the .ogg.');
}

main().catch((e) => { console.error(e); process.exit(1); });
