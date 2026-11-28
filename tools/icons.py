"""Writes the app's 24 dp icons as Android vectors from Tabler Icons (MIT) at one stroke width."""
import re, sys, pathlib
src, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
STROKE = "1.75"
MAP = {
    "ic_add": ("plus", "o"), "ic_add_photo": ("camera-plus", "o"), "ic_arrow_back": ("arrow-left", "o"),
    "ic_calendar": ("calendar", "o"), "ic_calendar_filled": ("calendar", "f"), "ic_calendar_plus": ("calendar-plus", "o"),
    "ic_check": ("check", "o"),
    "ic_chevron_left": ("chevron-left", "o"), "ic_chevron_right": ("chevron-right", "o"), "ic_close": ("x", "o"),
    "ic_content_paste": ("clipboard-text", "o"), "ic_delete": ("trash", "o"), "ic_document_scanner": ("scan", "o"),
    "ic_download": ("download", "o"), "ic_drag_handle": ("grip-vertical", "o"), "ic_edit": ("pencil", "o"),
    "ic_favorite": ("heart", "f"), "ic_favorite_border": ("heart", "o"), "ic_folder": ("folder", "o"),
    "ic_label": ("tag", "o"), "ic_link": ("link", "o"), "ic_lock": ("lock", "o"), "ic_more_vert": ("dots-vertical", "o"),
    "ic_person": ("user", "o"), "ic_play": ("player-play", "f"), "ic_remove": ("minus", "o"),
    "ic_remove_shopping_cart": ("shopping-cart-off", "o"), "ic_search": ("search", "o"), "ic_send": ("send", "o"),
    "ic_settings": ("settings", "o"),
    "ic_settings_filled": ("settings", "f"), "ic_share": ("share", "o"), "ic_shopping_cart": ("shopping-cart", "o"),
    "ic_sort": ("arrows-sort", "o"), "ic_star": ("star", "f"), "ic_star_border": ("star", "o"),
    "ic_stop": ("player-stop", "f"), "ic_timer": ("stopwatch", "o"), "ic_view": ("layout-grid", "o"),
}
# The family's own glyph, drawn for this app: the bowl of the launcher icon under three of its pieces.
BOWL = ["M3 13h18a9 9 0 0 1 -18 0z", "M4.5 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M13.47 6.81L10 5.87L10.93 2.4L14.4 3.33z", "M16.88 5.88L20.14 9.15L15.69 10.34z"]
HEAD = '<?xml version="1.0" encoding="utf-8"?>\n<!-- {note} -->\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="24dp"\n    android:height="24dp"\n    android:viewportWidth="24"\n    android:viewportHeight="24">\n'
def path(d, style):
    stroke = f'        android:strokeColor="@android:color/white"\n        android:strokeWidth="{STROKE}"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round"\n'
    fill = '        android:fillColor="@android:color/white"\n'
    body = {"o": stroke, "f": fill, "of": fill + stroke}[style]
    return f'    <path\n{body}        android:pathData="{d}" />\n'
def write(name, note, paths, style):
    (out / f"{name}.xml").write_text(HEAD.format(note=note) + "".join(path(d, style) for d in paths) + "</vector>\n")
for name, (icon, style) in MAP.items():
    svg = (src / ("outline" if style == "o" else "filled") / f"{icon}.svg").read_text()
    paths = [d for d in re.findall(r'<path[^>]*?\sd="([^"]+)"', svg) if d != "M0 0h24v24H0z"]
    assert paths, icon
    write(name, f"Tabler Icons “{icon}” (MIT), {'outline' if style == 'o' else 'filled'}", paths, style)
write("ic_cookbook", "The bowl of the app icon under three of its pieces, drawn for this app", BOWL, "o")
write("ic_cookbook_filled", "The bowl of the app icon under three of its pieces, filled", BOWL, "of")
print(len(MAP) + 2, "icons")
