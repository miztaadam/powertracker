"""Grit app-icon concepts: builds outlined SVG layers + montage HTML."""
import math, os, itertools
from fontTools.ttLib import TTFont
from fontTools.pens.transformPen import TransformPen
from pathops import Path, PathOp, op, LineCap, LineJoin
from fontTools.pens.basePen import BasePen
class Seg(BasePen):
    """Decomposes TrueType implied-point quadratics into single segments for pathops."""
    def __init__(self, pen, gs=None): BasePen.__init__(self, gs); self.p = pen
    def _moveTo(self, pt): self.p.moveTo(pt)
    def _lineTo(self, pt): self.p.lineTo(pt)
    def _curveToOne(self, a, b, c): self.p.curveTo(a, b, c)
    def _qCurveToOne(self, a, b): self.p.qCurveTo(a, b)
    def _closePath(self): self.p.closePath()
    def _endPath(self): self.p.endPath()

OUT = '/home/user/powertracker/docs/design/logo'
HERE = os.path.dirname(os.path.abspath(__file__))
FONT = TTFont('/home/user/powertracker/app/src/main/res/font/barlow_condensed_bold.ttf')
GS, CMAP, HMTX = FONT.getGlyphSet(), FONT.getBestCmap(), FONT['hmtx']
VOLT, INK, G0, G1, CHALK = '#C9F73A', '#121700', '#0B0C0E', '#131518', '#F3F4EF'
os.makedirs(OUT, exist_ok=True)


# ---------- path helpers ----------
def xf(p, m):
    q = Path(); p.draw(TransformPen(q.getPen(), m)); return q
def move(p, dx, dy): return xf(p, (1, 0, 0, 1, dx, dy))
def scale(p, sx, sy=None): return xf(p, (sx, 0, 0, sx if sy is None else sy, 0, 0))
def rot(p, deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return xf(p, (c, s, -s, c, 0, 0))
def skew(p, deg): return xf(p, (1, 0, -math.tan(math.radians(deg)), 1, 0, 0))  # y-down: lean right
def U(*ps):
    r = Path()
    for p in ps: r = op(r, p, PathOp.UNION)
    return r
def D(a, b): return op(a, b, PathOp.DIFFERENCE)
def I(a, b): return op(a, b, PathOp.INTERSECTION)
def stroke(p, w, round_=False):
    q = Path(); p.draw(q.getPen())
    q.stroke(w, LineCap.ROUND_CAP if round_ else LineCap.BUTT_CAP,
             LineJoin.ROUND_JOIN if round_ else LineJoin.MITER_JOIN, 4)
    q.simplify(); return q
def grow(p, e, round_=False): return U(p, stroke(p, 2 * e, round_))
def shrink(p, e): return D(p, stroke(p, 2 * e))
def poly(pts):
    p = Path(); pen = p.getPen(); pen.moveTo(pts[0])
    for q in pts[1:]: pen.lineTo(q)
    pen.closePath(); return p
def rect(x0, y0, x1, y1, r=0):
    if r <= 0: return poly([(x0, y0), (x1, y0), (x1, y1), (x0, y1)])
    r = min(r, (x1 - x0) / 2, (y1 - y0) / 2); k = r * 0.5523
    p = Path(); pen = p.getPen()
    pen.moveTo((x0 + r, y0)); pen.lineTo((x1 - r, y0))
    pen.curveTo((x1 - r + k, y0), (x1, y0 + r - k), (x1, y0 + r)); pen.lineTo((x1, y1 - r))
    pen.curveTo((x1, y1 - r + k), (x1 - r + k, y1), (x1 - r, y1)); pen.lineTo((x0 + r, y1))
    pen.curveTo((x0 + r - k, y1), (x0, y1 - r + k), (x0, y1 - r)); pen.lineTo((x0, y0 + r))
    pen.curveTo((x0, y0 + r - k), (x0 + r - k, y0), (x0 + r, y0)); pen.closePath(); return p
def circle(cx, cy, r): return rect(cx - r, cy - r, cx + r, cy + r, r)
def ring(cx, cy, r1, r0): return D(circle(cx, cy, r1), circle(cx, cy, r0))
def line(x0, y0, x1, y1, w):
    dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy); nx, ny = -dy / L * w / 2, dx / L * w / 2
    return poly([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)])
def pts(p):
    return [pt for _, ps in p.segments for pt in ps]
def bounds(ps):
    xs = [x for p in ps for x, _ in pts(p)]; ys = [y for p in ps for _, y in pts(p)]
    return min(xs), min(ys), max(xs), max(ys)

def glyph(ch, cap=100.0, xscale=1.0):
    """Glyph outline, baseline y=0, y-down, scaled so cap height == cap. Returns (path, advance)."""
    name = CMAP[ord(ch)]; s = cap / 700.0
    p = Path(); GS[name].draw(TransformPen(Seg(p.getPen(), GS), (s * xscale, 0, 0, -s, 0, 0)))
    p.simplify()
    return p, HMTX[name][0] * s * xscale
def text(t, cap=100.0, track=0.0, xscale=1.0):
    x = 0; out = []
    for ch in t:
        if ch == ' ': x += cap * 0.30 + track; continue
        g, adv = glyph(ch, cap, xscale); out.append(move(g, x, 0)); x += adv + track
    return U(*out), x - track
def arc_text(t, r, cap, track, top=True):
    """Outlined text on a circle centred at the origin. top: baseline radius r, reads clockwise.
    bottom: letters upright at the bottom, reading left-to-right, occupying the same radial band."""
    items = []
    for ch in t:
        if ch == ' ': items.append((None, cap * 0.34)); continue
        if ch == '·': items.append((circle(0, -cap / 2, cap * 0.13), cap * 0.5)); continue
        g, adv = glyph(ch, cap); items.append((g, adv))
    total = sum(a for _, a in items) + track * (len(items) - 1)
    rm = r + cap / 2
    ang = -total / rm / 2 if top else total / rm / 2
    out = []
    for g, adv in items:
        half = (adv / 2) / rm
        ang += half if top else -half
        if g is not None:
            g = move(g, -adv / 2, -r if top else r + cap)
            out.append(rot(g, math.degrees(ang)))
        ang += (half + track / rm) if top else -(half + track / rm)
    return U(*out)

def knurl_bar(x0, x1, y0, y1, sections, pitch, w):
    """Bar shaft: solid bar with diamond knurl cut into the given (start, end) fractions."""
    bar = rect(x0, y0, x1, y1, 0)
    cuts = Path(); W = x1 - x0; h = y1 - y0
    for a, b in sections:
        xa, xb = x0 + W * a, x0 + W * b
        clip = rect(xa, y0 - 1, xb, y1 + 1)
        ls = []
        x = xa - h * 2
        while x < xb + h * 2:
            ls.append(line(x, y1 + 1, x + (h + 2) * 0.8, y0 - 1, w))
            ls.append(line(x, y0 - 1, x + (h + 2) * 0.8, y1 + 1, w))
            x += pitch
        cuts = U(cuts, I(U(*ls), clip))
    return D(bar, cuts)

def fit(layers, radius=32.6, cx=54, cy=54):
    """Uniformly scale/translate layers [(path, fill)] so every point lies inside the safe circle."""
    ps = [p for p, _ in layers]
    x0, y0, x1, y1 = bounds(ps); mx, my = (x0 + x1) / 2, (y0 + y1) / 2
    rmax = max(math.hypot(x - mx, y - my) for p in ps for x, y in pts(p))
    s = radius / rmax
    return [(move(scale(move(p, -mx, -my), s), cx, cy), f) for p, f in layers]

# ---------- svg output ----------
def d_of(p):
    out = []
    f = lambda v: ('%.2f' % v).rstrip('0').rstrip('.')
    for verb, ps in p.segments:
        c = {'moveTo': 'M', 'lineTo': 'L', 'qCurveTo': 'Q', 'curveTo': 'C', 'closePath': 'Z', 'endPath': ''}[verb]
        if verb == 'qCurveTo' and len(ps) > 2:   # TrueType-style run: insert implied on-curve points
            q = []
            for i in range(len(ps) - 2):
                q += [ps[i], ((ps[i][0] + ps[i + 1][0]) / 2, (ps[i][1] + ps[i + 1][1]) / 2)]
            ps = q + list(ps[-2:])
        out.append(c + ' '.join(f(x) + ',' + f(y) for x, y in ps))
    return ''.join(out)
def body(layers):
    s = ''
    for item in layers:
        p, fill = item[0], item[1]; extra = item[2] if len(item) > 2 else ''
        if isinstance(p, str): s += p; continue
        s += '<path fill="%s"%s d="%s"/>\n' % (fill, extra, d_of(p))
    return s
def write(name, layers, title, vb='0 0 108 108'):
    w, h = vb.split()[2:]
    svg = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="%s" width="%s" height="%s" role="img">\n'
           '<title>%s</title>\n%s</svg>\n' % (vb, w, h, title, body(layers)))
    open(os.path.join(OUT, name), 'w').write(svg)

FULL = rect(0, 0, 108, 108)
concepts = []  # (id, name, blurb, bg_layers, fg_layers)

# ===== A · Knurl: heavy condensed italic caps over a knurled bar shaft =====
def concept_a():
    t, w = text('GRIT', 100, track=2, xscale=0.90)
    t = grow(t, 2.6)
    x0, _, x1, _ = bounds([t])
    bar = knurl_bar(x0, x1, 17, 33, [(0, .30), (.43, .57), (.70, 1)], 4.6, 0.8)
    t, bar = skew(t, 11), skew(bar, 11)
    fg = fit([(t, INK), (bar, INK)])
    tex = []
    x = -120
    while x < 230:
        tex.append(line(x, 110, x + 96, -2, 0.8)); tex.append(line(x, -2, x + 96, 110, 0.8)); x += 9
    bg = [(FULL, VOLT), (I(U(*tex), FULL), INK, ' fill-opacity="0.07"')]
    return bg, fg

# ===== B · Barbell I: the I is a loaded bar stood on end =====
def barbell(h=100.0, over=9.0, big=48.0, small=33.0, bar=13.0):
    """Vertical barbell occupying y in [-h-over, over], centred on x=0. Returns (steel, plates)."""
    steel = [rect(-bar / 2, -h - over, bar / 2, over, 2.5)]
    plates = []
    for sgn in (1, -1):
        def band(w, ya, yb, r):
            y0, y1 = (-h + ya, -h + yb) if sgn == 1 else (-yb, -ya)
            return rect(-w / 2, y0, w / 2, y1, r)
        plates.append(band(big, 0, 17, 3.5))
        plates.append(band(small, 20.5, 32, 3))
    return U(*steel), U(*plates)
def concept_b():
    gr, w1 = text('GR', 100, track=1)
    tt, w2 = text('T', 100)
    steel, plates = barbell()
    bx = w1 + 3 + 24
    steel, plates = move(steel, bx, 0), move(plates, bx, 0)
    tt = move(tt, bx + 24 + 3, 0)
    fg = fit([(U(gr, tt), CHALK), (steel, CHALK), (plates, VOLT)])
    return [(FULL, G1)], fg

# ===== C · Plate G: the G is a competition plate seen face-on =====
def plate_g(R=56.0, t=20.5):
    r = R - t
    g = ring(0, 0, R, r)
    hb = 9.5
    g = D(g, poly([(0, -hb), (R + 20, -hb), (R + 20, -hb - (R + 20) * math.tan(math.radians(40)))]))
    barp = I(U(rect(0, -hb, R + 5, hb), circle(0, 0, hb)), circle(0, 0, R))
    g = U(g, barp)
    g = D(g, circle(0, 0, 3.6))                       # bar hole in the hub
    return g
def concept_c():
    g = move(plate_g(), 56, -50)
    rit, w = text('RIT', 100, track=1)
    rit = move(rit, 112 + 3, 0)
    fg = fit([(g, VOLT), (rit, CHALK)])
    return [(FULL, G1)], fg

# ===== D · Stamp: the icon is a plate face; the word is cast into it =====
def concept_d():
    t, w = text('GRIT', 24.5, track=0.6)
    x0, y0, x1, y1 = bounds([t])
    t = move(t, 54 - (x0 + x1) / 2, 54 - (y0 + y1) / 2)
    top = move(arc_text('POWERLIFTING', 23.2, 4.7, 1.25, True), 54, 54)
    bot = move(arc_text('LOGBOOK', 23.2, 4.7, 1.9, False), 54, 54)
    rule = U(rect(54 - 20.5, 54 - 16.3, 54 + 20.5, 54 - 15.2), rect(54 - 20.5, 54 + 15.2, 54 + 20.5, 54 + 16.3))
    fg = [(t, INK), (U(top, bot), INK), (ring(54, 54, 32.4, 30.6), INK)]
    bg = [(FULL, VOLT), (ring(54, 54, 35.3, 34.5), INK, ' fill-opacity="0.4"')]
    return bg, fg

# ===== E · Plate dot: title-case Grit, the dot of the i is a plate =====
def concept_e():
    G_, a1 = glyph('G'); r_, a2 = glyph('r'); i_, a3 = glyph('i'); t_, a4 = glyph('t')
    # split the i into stem + dot, drop the dot
    segs = list(i_.segments); contours = []; cur = None
    for verb, ps in segs:
        if verb == 'moveTo': cur = Path(); contours.append(cur); pen = cur.getPen(); pen.moveTo(ps[0])
        elif verb == 'lineTo': pen.lineTo(ps[0])
        elif verb == 'qCurveTo': pen.qCurveTo(*ps)
        elif verb == 'curveTo': pen.curveTo(*ps)
        elif verb == 'closePath': pen.closePath()
    contours.sort(key=lambda c: bounds([c])[1])
    dot, stem = contours[0], contours[-1]
    sx0, sy0, sx1, sy1 = bounds([stem])
    x = 0; tr = 1.0
    parts = [G_]; x += a1 + tr
    parts.append(move(r_, x, 0)); x += a2 + tr
    stem = move(stem, x, 0); cx = x + (sx0 + sx1) / 2; parts.append(stem); x += a3 + tr
    parts.append(move(t_, x, 0))
    R = 15.5
    cy = sy0 - 6.5 - R
    plate = D(circle(cx, cy, R), ring(cx, cy, R - 5.6, R - 8.2))
    plate = D(plate, circle(cx, cy, 2.3))
    fg = fit([(U(*parts), CHALK), (plate, VOLT)])
    return [(FULL, G1)], fg

SPECS = [
    ('a-knurl', 'A · Knurl', 'Heavy condensed italic caps on a knurled bar shaft', concept_a),
    ('b-barbell', 'B · Barbell I', 'The I is a loaded bar stood on end', concept_b),
    ('c-plate', 'C · Plate G', 'The G is a competition plate, face-on', concept_c),
    ('d-stamp', 'D · Stamp', 'The whole icon is a plate face; the word is cast into it', concept_d),
    ('e-dot', 'E · Plate dot', 'Title-case Grit; the dot of the i is a plate', concept_e),
]

# masks in the 72 dp launcher viewport (18..90 of the 108 dp canvas)
def squircle_d(n=4.2, r=36, c=54, steps=96):
    out = []
    for k in range(steps):
        a = 2 * math.pi * k / steps; ca, sa = math.cos(a), math.sin(a)
        x = c + r * math.copysign(abs(ca) ** (2 / n), ca); y = c + r * math.copysign(abs(sa) ** (2 / n), sa)
        out.append('%.2f,%.2f' % (x, y))
    return 'M' + 'L'.join(out) + 'Z'
MASKS = {'circle': '<circle cx="54" cy="54" r="36"/>',
         'squircle': '<path d="%s"/>' % squircle_d(),
         'rounded': '<rect x="18" y="18" width="72" height="72" rx="17"/>'}
_uid = itertools.count()
def masked(inner, mask, px):
    i = next(_uid)
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72" width="%d" height="%d">'
            '<clipPath id="m%d">%s</clipPath><g clip-path="url(#m%d)">%s</g></svg>' % (px, px, i, MASKS[mask], i, inner))

rows = []
for cid, name, blurb, fn in SPECS:
    bg, fg = fn()
    # safe-zone check for the word
    rmax = max(math.hypot(x - 54, y - 54) for p, *_ in fg for x, y in pts(p))
    print(cid, 'max radius from centre %.2f (safe 33)' % rmax, 'bbox', ['%.1f' % v for v in bounds([p for p, *_ in fg])])
    mono = [(U(*[p for p, *_ in fg]), '#000000')]
    write(cid + '-icon-background.svg', bg, 'Grit icon %s - adaptive background layer (108 dp)' % name)
    write(cid + '-icon-foreground.svg', fg, 'Grit icon %s - adaptive foreground layer (108 dp, art inside the 66 dp safe zone)' % name)
    write(cid + '-icon-mono.svg', mono, 'Grit icon %s - monochrome layer for themed icons (108 dp)' % name)
    write(cid + '-icon.svg', bg + fg, 'Grit icon %s - flattened full-bleed preview' % name)
    rows.append((cid, name, blurb, body(bg) + body(fg), d_of(mono[0][0])))

# ---------- montage ----------
def montage(theme):
    dark = theme == 'dark'
    wall = 'linear-gradient(160deg,#343b45 0%,#1d2127 100%)' if dark else 'linear-gradient(160deg,#F7F7F3 0%,#DADFE2 100%)'
    fgc, mut, hair = ('#F3F4EF', '#959BA4', '#262A30') if dark else ('#111315', '#595E66', '#CFCFC8')
    tb, tf = ('#14171B', '#D5E89A') if dark else ('#E6F9A8', '#3F6100')
    h = ['<!doctype html><meta charset="utf-8"><style>',
         '@font-face{font-family:BC;src:url(file:///home/user/powertracker/app/src/main/res/font/barlow_condensed_bold.ttf)}',
         '@font-face{font-family:B;src:url(file:///home/user/powertracker/app/src/main/res/font/barlow_semibold.ttf)}',
         'body{margin:0;width:1500px;background:%s;color:%s;font-family:B,sans-serif}' % (wall, fgc),
         '.wrap{padding:36px 44px 28px}h1{font:700 40px/1 BC;margin:0 0 6px}.sub{color:%s;font-size:15px;margin-bottom:22px}' % mut,
         '.row{border-top:1px solid %s;padding:18px 0 20px}' % hair,
         '.name{font:700 28px/1 BC}.blurb{color:%s;font-size:14px;margin:4px 0 14px}' % mut,
         '.cells{display:flex;gap:34px;align-items:flex-end}.grp{display:flex;gap:16px;align-items:flex-end}',
         '.cell{display:flex;flex-direction:column;align-items:center;gap:6px}.cell span,.eyebrow{font-size:11px;letter-spacing:1.1px;text-transform:uppercase;color:%s}' % mut,
         '.col{display:flex;flex-direction:column;gap:8px}svg{display:block}',
         '</style><div class="wrap"><h1>Grit — app icon concepts</h1>',
         '<div class="sub">Adaptive icon (108 dp canvas, 72 dp viewport, word inside the 66 dp safe zone) · %s wallpaper · circle, squircle and rounded-square masks at 192 / 96 / 48 px · themed (monochrome) icon</div>' % theme]
    for cid, name, blurb, inner, monod in rows:
        h.append('<div class="row"><div class="name">%s</div><div class="blurb">%s · <code>%s-icon-*.svg</code></div><div class="cells">' % (name, blurb, cid))
        for mask, label in (('circle', 'Circle'), ('squircle', 'Squircle'), ('rounded', 'Rounded square')):
            h.append('<div class="col"><div class="eyebrow">%s</div><div class="grp">' % label)
            for px in (192, 96, 48):
                h.append('<div class="cell">%s<span>%d</span></div>' % (masked(inner, mask, px), px))
            h.append('</div></div>')
        themed = '<rect width="108" height="108" fill="%s"/><path fill="%s" d="%s"/>' % (tb, tf, monod)
        h.append('<div class="col"><div class="eyebrow">Themed</div><div class="grp">')
        for px in (96, 48):
            h.append('<div class="cell">%s<span>%d</span></div>' % (masked(themed, 'circle', px), px))
        h.append('</div></div></div></div>')
    h.append('</div>')
    open(os.path.join(HERE, 'montage-%s.html' % theme), 'w').write('\n'.join(h))
montage('dark'); montage('light')

# ===== FINAL · B Barbell I in steel grey (owner's choice: no volt) =====
STEEL_LIGHT, STEEL_DARK = '#B4BAC2', '#7E858F'   # plates = the design system's plate grey; bar a step darker
def concept_b_grey():
    gr, w1 = text('GR', 100, track=1)
    tt, w2 = text('T', 100)
    steel, plates = barbell()
    bx = w1 + 3 + 24
    steel, plates = move(steel, bx, 0), move(plates, bx, 0)
    tt = move(tt, bx + 24 + 3, 0)
    return [(FULL, G1)], fit([(U(gr, tt), CHALK), (steel, STEEL_DARK), (plates, STEEL_LIGHT)])
def final_b_grey():
    cid, name = 'b-barbell-grey', 'B - Barbell I, steel grey (final)'
    bg, fg = concept_b_grey()
    mono = [(U(*[p for p, *_ in fg]), '#000000')]
    write(cid + '-icon-background.svg', bg, 'Grit icon %s - adaptive background layer (108 dp)' % name)
    write(cid + '-icon-foreground.svg', fg, 'Grit icon %s - adaptive foreground layer (108 dp, art inside the 66 dp safe zone)' % name)
    write(cid + '-icon-mono.svg', mono, 'Grit icon %s - monochrome layer for themed icons (108 dp)' % name)
    write(cid + '-icon.svg', bg + fg, 'Grit icon %s - flattened full-bleed preview' % name)
    inner, monod = body(bg) + body(fg), d_of(mono[0][0])
    for theme in ('dark', 'light'):
        dark = theme == 'dark'
        wall = 'linear-gradient(160deg,#343b45 0%,#1d2127 100%)' if dark else 'linear-gradient(160deg,#F7F7F3 0%,#DADFE2 100%)'
        fgc, mut = ('#F3F4EF', '#959BA4') if dark else ('#111315', '#595E66')
        tb, tf = ('#14171B', '#D5E89A') if dark else ('#E6F9A8', '#3F6100')
        themed = '<rect width="108" height="108" fill="%s"/><path fill="%s" d="%s"/>' % (tb, tf, monod)
        h = ['<!doctype html><meta charset="utf-8"><style>',
             '@font-face{font-family:BC;src:url(file:///home/user/powertracker/app/src/main/res/font/barlow_condensed_bold.ttf)}',
             '@font-face{font-family:B;src:url(file:///home/user/powertracker/app/src/main/res/font/barlow_semibold.ttf)}',
             'body{margin:0;width:1500px;background:%s;color:%s;font-family:B,sans-serif}' % (wall, fgc),
             '.wrap{padding:36px 44px 40px;display:flex;gap:56px;align-items:flex-start}h1{font:700 40px/1 BC;margin:0 0 6px}',
             '.sub{color:%s;font-size:15px;margin-bottom:26px}' % mut,
             '.cells{display:flex;gap:30px;align-items:flex-end;margin-bottom:26px}.grp{display:flex;gap:16px;align-items:flex-end}',
             '.cell{display:flex;flex-direction:column;align-items:center;gap:6px}.cell span,.eyebrow{font-size:11px;letter-spacing:1.1px;text-transform:uppercase;color:%s}' % mut,
             '.col{display:flex;flex-direction:column;gap:8px}svg{display:block}</style><div class="wrap">',
             '<div>%s</div><div><h1>Grit — final icon · Barbell I, steel grey</h1>' % masked(inner, 'rounded', 480),
             '<div class="sub">Chalk G, R, T on graphite · plates %s, bar %s · %s wallpaper</div>' % (STEEL_LIGHT, STEEL_DARK, theme)]
        for mask, label in (('circle', 'Circle'), ('squircle', 'Squircle'), ('rounded', 'Rounded square')):
            h.append('<div class="cells"><div class="col"><div class="eyebrow">%s</div><div class="grp">' % label)
            for px in (192, 96, 48):
                h.append('<div class="cell">%s<span>%d</span></div>' % (masked(inner, mask, px), px))
            h.append('</div></div>')
            if mask == 'circle':
                h.append('<div class="col"><div class="eyebrow">Themed</div><div class="grp">')
                for px in (96, 48):
                    h.append('<div class="cell">%s<span>%d</span></div>' % (masked(themed, 'circle', px), px))
                h.append('</div></div>')
            h.append('</div>')
        h.append('</div></div>')
        open(os.path.join(HERE, 'final-%s.html' % theme), 'w').write('\n'.join(h))
final_b_grey()
