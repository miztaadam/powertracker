import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../../db/db'
import { grit } from '../../domain'
import { useExerciseBrowser, type StoredExercise } from '../../state/exercisePicker'
import { useToday } from '../../state/useToday'
import { TODAY, todayFixture } from '../../test/todayFixture'
import { ExercisePicker } from './ExercisePicker'
import { ExercisePicture, ExerciseThumbnail, pictureLabel } from './ExercisePicture'
import { ExercisesScreen } from './ExercisesScreen'
import { FormGuideSheet, NO_GUIDE } from './FormGuideSheet'

// Ported from Android's ExercisePictureTest, under the same names. "Bench" is the exercise with a figure; a lookup
// that finds nothing stands in for one without. jsdom lays nothing out and resolves no CSS variables, so the checks
// that need real sizes and real colours are made again, under the same names, in e2e/pictures.spec.ts.

const bench = grit.figureInfo('Bench')!
const LABEL = `Picture: ${bench.startCaption}, then ${bench.endCaption}.`

const read = (path: string) => readFileSync(resolve(process.cwd(), path), 'utf8')
const figureCss = read('src/screens/library/figure.css')
const libraryCss = read('src/screens/library/library.css')
const tokensCss = read('src/ui/tokens.css')

/** The declarations of the rule for exactly `selector`, as written. */
function rule(css: string, selector: string): string {
  const found = new RegExp(`(?:^|[},])\\s*${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*(?:,[^{]*)?\\{([^}]*)\\}`, 'm').exec(css)
  if (!found) throw new Error(`no rule for ${selector}`)
  return found[1]!
}

const sheet = (name: string) => screen.getByRole('dialog', { name: `Form guide for ${name}` })
const picture = (root: HTMLElement) => root.querySelector<HTMLElement>('.lib-picture')
const thumbnails = (root: ParentNode = document) => Array.from(root.querySelectorAll<HTMLElement>('.lib-thumb'))
const shapesIn = (svg: Element) => Array.from(svg.children)

/** What an SVG element is drawn from: its tag and the role its class names. */
const drawn = (svg: Element) => shapesIn(svg).map((el) => `${el.tagName.toLowerCase()} ${el.getAttribute('class')}`)
/** The same for the domain's shapes, in the renderer's order. */
const TAG: Record<string, string> = { circle: 'circle', capsule: 'line', rect: 'rect', polygon: 'polygon' }
const expected = (name: string, which: 'start' | 'end') => grit.figureShapes(name, which)!.map((s) => `${TAG[s.kind]} fig-${s.role.toLowerCase().replace('_', '-')}`)

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
  vi.restoreAllMocks()
})

const ownExercise = () => db.exercises.add({ name: 'Tyre flip', category: 'ACCESSORY', parentLift: null, muscles: ['Quads', 'Glutes'], isBuiltIn: false, archived: false })

/** The Exercises tab over the seeded exercises. */
async function showExercisesTab() {
  function Harness() {
    const session = useToday(db, () => TODAY)
    const browser = useExerciseBrowser()
    return <ExercisesScreen db={db} session={session} browser={browser} today={() => TODAY} onOpenPlan={() => {}} onOpenToday={() => {}} />
  }
  render(<Harness />)
  await screen.findByRole('listitem', { name: 'Squat' })
}

const row = (name: string) => screen.getByRole('listitem', { name })

describe('exercise pictures', () => {
  it('guideSheet_showsTwoPosePicture_withCaptions', () => {
    render(<FormGuideSheet name="Bench" onClose={() => {}} />)
    const shown = picture(sheet('Bench'))!
    expect(shown).not.toBeNull()

    // Two panels side by side, start then end, each a drawing with its caption underneath.
    const panels = Array.from(shown.querySelectorAll<HTMLElement>('.lib-picture-panel'))
    expect(panels).toHaveLength(2)
    expect(panels.map((p) => p.querySelector('.lib-picture-caption')!.textContent)).toEqual([bench.startCaption, bench.endCaption])
    for (const panel of panels) {
      expect(Array.from(panel.children).map((el) => el.tagName.toLowerCase())).toEqual(['svg', 'span'])
      expect(panel.querySelector('svg')).toHaveAttribute('viewBox', `0 0 ${bench.viewBox[0]} ${bench.viewBox[1]}`)
    }
    expect(bench.viewBox).toEqual([400, 300])

    // Each panel draws its pose's shapes, every one, in the renderer's order.
    const [start, end] = panels.map((p) => p.querySelector('svg')!) as [SVGSVGElement, SVGSVGElement]
    expect(drawn(start)).toEqual(expected('Bench', 'start'))
    expect(drawn(end)).toEqual(expected('Bench', 'end'))
    expect(drawn(start).length).toBeGreaterThan(10)
    expect(start.innerHTML).not.toBe(end.innerHTML)
    // A plate ring is one polygon, its hole wound the other way: it needs the non-zero rule.
    const polygons = Array.from(shown.querySelectorAll('polygon'))
    expect(polygons.length).toBeGreaterThan(0)
    for (const polygon of polygons) expect(polygon).toHaveAttribute('fill-rule', 'nonzero')
    // Capsules are round-capped lines of their own width.
    const capsule = grit.figureShapes('Bench', 'start')!.find((s) => s.kind === 'capsule') as unknown as { x1: number; width: number }
    const line = start.querySelector('line')!
    expect(line).toHaveAttribute('stroke-linecap', 'round')
    expect(Number(line.getAttribute('stroke-width'))).toBe(capsule.width)
    expect(Number(line.getAttribute('x1'))).toBe(capsule.x1)

    // Under the name and the area and equipment line, above the purpose.
    const order = (a: Element, b: Element) => Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING)
    const inSheet = within(sheet('Bench'))
    expect(order(inSheet.getByRole('heading', { level: 2, name: 'Bench' }), shown)).toBe(true)
    expect(order(inSheet.getByText('Main lifts · Barbell'), shown)).toBe(true)
    expect(order(shown, inSheet.getByText(grit.findInLibrary('Bench')!.guide.purpose))).toBe(true)
  })

  it('guideSheet_pictureLabel_readsBothCaptions', () => {
    expect(pictureLabel(bench)).toBe(LABEL)
    render(<FormGuideSheet name="Bench" onClose={() => {}} />)
    // One element to a screen reader: an image with both captions in its name, and nothing inside it to stop on.
    const images = within(sheet('Bench')).getAllByRole('img')
    expect(images).toHaveLength(1)
    expect(images[0]).toBe(picture(sheet('Bench')))
    expect(images[0]).toHaveAccessibleName(LABEL)
    for (const svg of images[0]!.querySelectorAll('svg')) expect(svg).toHaveAttribute('aria-hidden', 'true')
    for (const caption of images[0]!.querySelectorAll('.lib-picture-caption')) expect(caption).toHaveAttribute('aria-hidden', 'true')
    // An alias reads the same picture.
    cleanup()
    const alias = grit.findInLibrary('Bench')!.aliases[0]!
    render(<FormGuideSheet name={alias} onClose={() => {}} />)
    expect(within(sheet(alias)).getByRole('img')).toHaveAccessibleName(LABEL)
  })

  it('guideSheet_noFigure_showsNoPictureArea', () => {
    // A catalogue exercise whose figure the lookup does not find: the guide, and no picture area at all.
    const lookup = vi.spyOn(grit, 'figureInfo').mockReturnValue(null)
    render(<FormGuideSheet name="Bench" onClose={() => {}} />)
    expect(lookup).toHaveBeenCalledWith('Bench')
    expect(picture(sheet('Bench'))).toBeNull()
    expect(sheet('Bench').querySelector('svg.fig')).toBeNull()
    expect(within(sheet('Bench')).queryByRole('img')).toBeNull()
    expect(within(sheet('Bench')).getByText(grit.findInLibrary('Bench')!.guide.purpose)).toBeInTheDocument()
    cleanup()
    lookup.mockRestore()

    // The lifter's own exercise keeps its one line.
    render(<FormGuideSheet name="Tyre flip" onClose={() => {}} />)
    expect(picture(sheet('Tyre flip'))).toBeNull()
    expect(within(sheet('Tyre flip')).getByText(NO_GUIDE)).toBeInTheDocument()
  })

  it('exercisesTab_rowsShowThumbnail', async () => {
    await showExercisesTab()
    // Every row leads with one tile, inside the row's button and before its name.
    const rows = screen.getAllByRole('listitem')
    expect(rows.length).toBeGreaterThanOrEqual(grit.library().length)
    for (const item of rows) {
      const tiles = thumbnails(item)
      expect(tiles, item.getAttribute('aria-label')!).toHaveLength(1)
      expect(tiles[0]!.parentElement).toBe(item.querySelector('.lib-row-main'))
      expect(tiles[0]!.parentElement!.firstElementChild).toBe(tiles[0])
      // Decorative: the row already names the exercise.
      expect(tiles[0]).toHaveAttribute('aria-hidden', 'true')
    }
    // Bench's tile draws its chosen pose.
    const svg = thumbnails(row('Bench'))[0]!.querySelector('svg')!
    expect(svg).toHaveAttribute('aria-hidden', 'true')
    expect(svg).toHaveAttribute('viewBox', '0 0 400 300')
    expect(['start', 'end']).toContain(bench.thumbnail)
    expect(drawn(svg)).toEqual(expected('Bench', bench.thumbnail as 'start' | 'end'))
    // The row is still named by its button alone.
    expect(within(row('Bench')).getByRole('button', { name: 'Open Bench' })).toBeInTheDocument()
    expect(within(row('Bench')).queryByRole('img')).toBeNull()
  })

  it('exercisesTab_thumbnailFarOffScreen_isDrawnWhenItsRowComesNear', () => {
    // A browser that lays rows out: this one's row is far below the screen.
    let seen: ((entries: Partial<IntersectionObserverEntry>[]) => void) | undefined
    const observed: Element[] = []
    const unobserved: Element[] = []
    vi.stubGlobal(
      'IntersectionObserver',
      class {
        constructor(callback: (entries: Partial<IntersectionObserverEntry>[]) => void) {
          seen = callback
        }
        observe = (el: Element) => observed.push(el)
        unobserve = (el: Element) => unobserved.push(el)
      },
    )
    const farBelow = vi.spyOn(Element.prototype, 'getBoundingClientRect').mockReturnValue({ top: 9000, bottom: 9064, left: 0, right: 375, width: 375, height: 64, x: 0, y: 9000, toJSON: () => ({}) })
    render(
      <ul>
        <li className="lib-row">
          <ExerciseThumbnail name="Bench" />
        </li>
        <li className="lib-row">
          <ExerciseThumbnail />
        </li>
      </ul>,
    )
    // The tile is there, to keep the name in place, but its drawing is not made yet. The row is what is watched;
    // a tile with nothing to draw waits for nothing.
    const [tile, empty] = thumbnails() as [HTMLElement, HTMLElement]
    expect(tile.childElementCount).toBe(0)
    expect(observed).toEqual([tile.closest('.lib-row')])

    // Still far away: nothing. Near: drawn, and no longer watched.
    act(() => seen!([{ target: observed[0]!, isIntersecting: false }]))
    expect(tile.childElementCount).toBe(0)
    act(() => seen!([{ target: observed[0]!, isIntersecting: true }]))
    expect(drawn(tile.querySelector('svg')!)).toEqual(expected('Bench', bench.thumbnail as 'start' | 'end'))
    expect(unobserved).toContain(observed[0])
    expect(empty.childElementCount).toBe(0)
    farBelow.mockRestore()
    vi.unstubAllGlobals()
  })

  it('exercisesTab_customExercise_showsEmptyTile', async () => {
    await ownExercise()
    await showExercisesTab()
    await waitFor(() => expect(row('Tyre flip')).toBeInTheDocument())
    const own = thumbnails(row('Tyre flip'))
    expect(own).toHaveLength(1)
    expect(own[0]!.childElementCount).toBe(0)
    expect(own[0]).toHaveAttribute('aria-hidden', 'true')
    // The same tile as a drawn one, so names stay aligned down the list.
    expect(own[0]!.className).toBe(thumbnails(row('Bench'))[0]!.className)
    expect(thumbnails(row('Bench'))[0]!.childElementCount).toBe(1)
    cleanup()

    // A catalogue exercise with no figure gets the same empty tile; so does no name at all.
    vi.spyOn(grit, 'figureInfo').mockReturnValue(null)
    render(
      <>
        <ExerciseThumbnail name="Bench" />
        <ExerciseThumbnail />
      </>,
    )
    expect(thumbnails()).toHaveLength(2)
    for (const tile of thumbnails()) expect(tile.childElementCount).toBe(0)
  })

  it('picker_hasNoThumbnails', async () => {
    const exercises = (await db.exercises.toArray()) as StoredExercise[]
    render(<ExercisePicker title="Add exercise" exercises={exercises} recentIds={[]} onPick={() => {}} onCreateCustom={() => {}} onDismiss={() => {}} />)
    const picker = screen.getByRole('dialog', { name: 'Add exercise' })
    expect(within(picker).getByRole('listitem', { name: 'Bench' })).toBeInTheDocument()
    expect(thumbnails()).toHaveLength(0)
    expect(document.querySelector('svg.fig')).toBeNull()
  })

  it('picture_noOverflow_atPhoneWidth', () => {
    // Measured at 375 px in e2e/pictures.spec.ts. Here: nothing in the picture has a size of its own to overflow with.
    render(<ExercisePicture name="Bench" />)
    for (const svg of document.querySelectorAll('svg.fig')) {
      expect(svg).not.toHaveAttribute('width')
      expect(svg).not.toHaveAttribute('height')
    }
    // Two equal columns that may shrink below their content, and drawings as wide as their column.
    expect(rule(libraryCss, '.lib-picture')).toMatch(/grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
    expect(rule(libraryCss, '.lib-picture .fig')).toMatch(/width:\s*100%/)
    expect(rule(libraryCss, '.lib-picture .fig')).toMatch(/aspect-ratio:\s*4\s*\/\s*3/)
    // The thumbnail is 56 × 40 and never squeezed by a long name.
    const thumb = rule(libraryCss, '.lib-thumb')
    expect(thumb).toMatch(/width:\s*56px/)
    expect(thumb).toMatch(/height:\s*40px/)
    expect(thumb).toMatch(/flex:\s*none/)
  })

  it('picture_contrast_darkAndLight', () => {
    // The roles are drawn in these tokens ...
    expect(rule(figureCss, '.fig-body')).toMatch(/color:\s*var\(--text\)/)
    expect(rule(figureCss, '.fig-body-far')).toMatch(/color:\s*color-mix\(in srgb, var\(--text\) 55%, var\(--fig-surface, var\(--surface\)\)\)/)
    expect(rule(figureCss, '.fig-equipment')).toMatch(/color:\s*var\(--text-muted\)/)
    expect(rule(figureCss, '.fig-accent')).toMatch(/color:\s*var\(--accent\)/)
    expect(rule(figureCss, '.fig-accent-fill')).toMatch(/color:\s*var\(--accent\)/)
    expect(rule(figureCss, '.fig-accent-fill')).toMatch(/fill-opacity:\s*\.10/)
    expect(rule(figureCss, '.fig-gap')).toMatch(/color:\s*var\(--fig-surface, var\(--surface\)\)/)
    // ... on the sheet's surface, or on the thumbnail's tile.
    expect(read('src/ui/components/Dialog.css')).toMatch(/\.gr-dialog-sheet\s*\{[^}]*background:\s*var\(--surface\)/)
    expect(rule(libraryCss, '.lib-thumb')).toMatch(/background:\s*var\(--container-high\)/)
    expect(rule(libraryCss, '.lib-thumb')).toMatch(/--fig-surface:\s*var\(--container-high\)/)

    // ... and each of body, equipment and accent is at least 3:1 against both, dark and light.
    const vars = (block: string) => Object.fromEntries(Array.from(block.matchAll(/(--[a-z0-9-]+)\s*:\s*(#[0-9a-fA-F]{6})\s*;/g), (m) => [m[1]!, m[2]!]))
    const dark = vars(/:root\s*\{([^}]*)\}/.exec(tokensCss)![1]!)
    const light = { ...dark, ...vars(/@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\s*\{([^}]*)\}/.exec(tokensCss)![1]!) }
    expect(light['--surface']).not.toBe(dark['--surface'])
    const luminance = (hex: string) => {
      const [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255).map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)) as [number, number, number]
      return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    const ratio = (a: string, b: string) => (Math.max(luminance(a), luminance(b)) + 0.05) / (Math.min(luminance(a), luminance(b)) + 0.05)
    for (const [scheme, theme] of [['dark', dark], ['light', light]] as const)
      for (const surface of ['--surface', '--container-high'])
        for (const role of ['--text', '--text-muted', '--accent']) expect(ratio(theme[role]!, theme[surface]!), `${role} on ${surface}, ${scheme}`).toBeGreaterThanOrEqual(3)
  })

  it('picture_largeText_captionsWrap', () => {
    // Measured at 200% in e2e/pictures.spec.ts. Here: the caption is whole text that may wrap, with nothing to cut it.
    render(<ExercisePicture name="Bench" />)
    const captions = Array.from(document.querySelectorAll<HTMLElement>('.lib-picture-caption'))
    expect(captions.map((c) => c.textContent)).toEqual([bench.startCaption, bench.endCaption])
    for (const caption of captions) expect(caption.className).not.toMatch(/eyebrow/)
    const caption = rule(libraryCss, '.lib-picture-caption')
    expect(caption).toMatch(/overflow-wrap:\s*anywhere/)
    expect(caption).not.toMatch(/nowrap|ellipsis|line-clamp|overflow:\s*hidden|max-height/)
    expect(rule(libraryCss, '.lib-picture-panel')).toMatch(/min-width:\s*0/)
  })

  it('picture_followsThemeChange', () => {
    // Seen recolouring under an open sheet in e2e/pictures.spec.ts. Here: no colour is written into the drawing. Every
    // shape names a role, and a role is a token, which the theme resolves when it draws.
    render(
      <>
        <ExercisePicture name="Bench" />
        <ExerciseThumbnail name="Bench" />
      </>,
    )
    const shapes = Array.from(document.querySelectorAll('svg.fig > *'))
    expect(shapes.length).toBeGreaterThan(30)
    for (const shape of shapes) {
      expect(shape.getAttribute('class')).toMatch(/^fig-(body|body-far|equipment|accent|accent-fill|gap)$/)
      expect(shape).not.toHaveAttribute('style')
      for (const paint of ['fill', 'stroke']) expect([null, 'none', 'currentColor']).toContain(shape.getAttribute(paint))
    }
    expect(figureCss.replace(/\/\*[\s\S]*?\*\//g, '')).not.toMatch(/#[0-9a-fA-F]{3,8}\b|rgb\(|hsl\(/)
    for (const role of ['body', 'body-far', 'equipment', 'accent', 'accent-fill', 'gap']) expect(rule(figureCss, `.fig-${role}`)).toMatch(/color:\s*(var|color-mix)\(/)
  })
})
