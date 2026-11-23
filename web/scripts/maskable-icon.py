# Makes public/icons/v2/icon-maskable-512.png from public/icons/icon-512.png: the art shrunk to 80% and
# centred on the icon's own solid background, so a round or squircle mask never cuts it. A new file name, so
# installed copies and the service worker pick it up. Run from web/: python3 scripts/maskable-icon.py
import sys
from PIL import Image
src = sys.argv[1] if len(sys.argv) > 1 else "public/icons/icon-512.png"
out = sys.argv[2] if len(sys.argv) > 2 else "public/icons/v2/icon-maskable-512.png"
im = Image.open(src).convert('RGB')
assert im.size == (512, 512)
bg = im.getpixel((0, 0))
edges = [(x, y) for x in (0, 255, 511) for y in (0, 255, 511) if (x, y) != (255, 255)]
assert all(im.getpixel(p) == bg for p in edges), 'background is not solid'
size = round(512 * 0.8)  # 410: the art at 80%, inside the maskable safe zone
small = im.resize((size, size), Image.LANCZOS)
canvas = Image.new('RGB', (512, 512), bg)
off = (512 - size) // 2
canvas.paste(small, (off, off))
canvas.save(out, optimize=True)
print('bg', bg, 'art at', size, 'offset', off)
