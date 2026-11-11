import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

type Header = { key: string; value: string }
const config = JSON.parse(readFileSync(resolve(process.cwd(), 'vercel.json'), 'utf8')) as { headers: { source: string; headers: Header[] }[] }
const csp = config.headers.find((h) => h.source === '/(.*)')?.headers.find((h) => h.key === 'Content-Security-Policy')?.value ?? ''
const directives = new Map(csp.split(';').map((d) => d.trim().split(/\s+/)).map(([name, ...values]) => [name, values.join(' ')]))

describe('Content-Security-Policy (vercel.json)', () => {
  it('allows only Grit\'s own origin', () => {
    expect(directives.get('default-src')).toBe("'self'")
    expect(directives.get('connect-src')).toBe("'self'")
    expect(directives.has('script-src')).toBe(false)
  })
  it('cannot be framed, rebased or made to post a form elsewhere', () => {
    expect(directives.get('frame-ancestors')).toBe("'none'")
    expect(directives.get('base-uri')).toBe("'self'")
    expect(directives.get('form-action')).toBe("'self'")
  })
})
