#!/usr/bin/env python3
"""
Generate Android VectorDrawable XML files from Material Design Icons SVGs.
Downloads SVGs from Google's material-design-icons GitHub repo.
"""

import os
import re
import time
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

# Mapping of Compose icon name (PascalCase) -> (category, snake_case_name)
ICON_MAP = {
    # Filled icons (49)
    "AccountBalance":       ("action", "account_balance"),
    "Add":                  ("content", "add"),
    "Archive":              ("content", "archive"),
    "ArrowDropDown":        ("navigation", "arrow_drop_down"),
    "Assessment":           ("action", "assessment"),
    "AutoAwesome":          ("image", "auto_awesome"),
    "Bookmark":             ("action", "bookmark"),
    "CalendarMonth":        ("action", "calendar_month"),
    "ChatBubble":           ("communication", "chat_bubble"),
    "ChatBubbleOutline":    ("communication", "chat_bubble_outline"),
    "Check":                ("navigation", "check"),
    "CheckBox":             ("toggle", "check_box"),
    "CheckCircle":          ("action", "check_circle"),
    "Clear":                ("content", "clear"),
    "Close":                ("navigation", "close"),
    "ContentCopy":          ("content", "content_copy"),
    "DarkMode":             ("device", "dark_mode"),
    "Dashboard":            ("action", "dashboard"),
    "DateRange":            ("action", "date_range"),
    "Delete":               ("action", "delete"),
    "DeleteForever":        ("action", "delete_forever"),
    "Description":          ("action", "description"),
    "Done":                 ("action", "done"),
    "Download":             ("file", "download"),
    "Edit":                 ("image", "edit"),
    "Event":                ("action", "event"),
    "ExpandMore":           ("navigation", "expand_more"),
    "FileDownload":         ("file", "file_download"),
    "FitnessCenter":        ("places", "fitness_center"),
    "Info":                 ("action", "info"),
    "KeyboardArrowRight":   ("hardware", "keyboard_arrow_right"),
    "Label":                ("action", "label"),
    "LightMode":            ("device", "light_mode"),
    "Lightbulb":            ("action", "lightbulb"),
    "Link":                 ("content", "link"),
    "LinkOff":              ("content", "link_off"),
    "LocalLibrary":         ("maps", "local_library"),
    "Menu":                 ("navigation", "menu"),
    "MoreVert":             ("navigation", "more_vert"),
    "People":               ("social", "people"),
    "Psychology":           ("social", "psychology"),
    "PushPin":              ("content", "push_pin"),
    "RadioButtonUnchecked": ("toggle", "radio_button_unchecked"),
    "Receipt":              ("action", "receipt"),
    "Restore":              ("action", "restore"),
    "Save":                 ("content", "save"),
    "Search":               ("action", "search"),
    "Settings":             ("action", "settings"),
    "SettingsBrightness":   ("action", "settings_brightness"),
    "Share":                ("social", "share"),
    "Undo":                 ("content", "undo"),
    "Visibility":           ("action", "visibility"),
    "VisibilityOff":        ("action", "visibility_off"),
    "TextFields":           ("editor", "text_fields"),

    # Outlined icons (12)
    "AutoAwesomeOutlined":          ("image", "auto_awesome"),
    "CalendarMonthOutlined":        ("action", "calendar_month"),
    "CheckCircleOutlined":          ("action", "check_circle"),
    "DeleteOutlined":               ("action", "delete"),
    "EditOutlined":                 ("image", "edit"),
    "HistoryOutlined":              ("action", "history"),
    "MenuOutlined":                 ("navigation", "menu"),
    "PersonOutlined":               ("social", "person"),
    "PsychologyOutlined":           ("social", "psychology"),
    "ReceiptOutlined":              ("action", "receipt"),
    "RefreshOutlined":              ("navigation", "refresh"),
    "SearchOutlined":               ("action", "search"),

    # AutoMirrored.Filled icons (4)
    "ArrowBack":                    ("navigation", "arrow_back"),
    "KeyboardArrowRightMirrored":   ("hardware", "keyboard_arrow_right"),
    "KeyboardReturn":               ("hardware", "keyboard_return"),
    "LabelMirrored":                ("action", "label"),

    # AutoMirrored.Outlined icons (2)
    "ChatOutlined":                 ("communication", "chat"),
    "NoteAddOutlined":              ("action", "note_add"),
}

GITHUB_BASE = "https://raw.githubusercontent.com/google/material-design-icons/master/src"

OUTPUT_DIR = Path(__file__).parent.parent / "app" / "src" / "main" / "res" / "drawable"

SVG_NS = "http://www.w3.org/2000/svg"


def download_svg(category, icon_name, style, retries=3):
    """Download SVG from GitHub with retry logic."""
    url = f"{GITHUB_BASE}/{category}/{icon_name}/{style}/24px.svg"
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=20) as resp:
                return resp.read().decode("utf-8")
        except Exception as e:
            if attempt < retries - 1:
                time.sleep(1)
            else:
                print(f"  ERROR downloading {url}: {e}")
                return None


def rect_to_path(elem):
    """Convert SVG <rect> to path data."""
    x = float(elem.get("x", 0))
    y = float(elem.get("y", 0))
    w = float(elem.get("width", 0))
    h = float(elem.get("height", 0))
    return f"M{x},{y} h{w} v{h} h{-w} Z"


def polygon_to_path(elem):
    """Convert SVG <polygon> to path data."""
    points_str = elem.get("points", "").strip()
    if not points_str:
        return ""
    points = points_str.replace(",", " ").split()
    if len(points) < 4:
        return ""
    d = "M" + points[0] + "," + points[1]
    for i in range(2, len(points), 2):
        d += " L" + points[i] + "," + points[i + 1]
    d += " Z"
    return d


def circle_to_path(elem):
    """Convert SVG <circle> to path data."""
    cx = float(elem.get("cx", 0))
    cy = float(elem.get("cy", 0))
    r = float(elem.get("r", 0))
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0"


def line_to_path(elem):
    """Convert SVG <line> to path data."""
    x1 = float(elem.get("x1", 0))
    y1 = float(elem.get("y1", 0))
    x2 = float(elem.get("x2", 0))
    y2 = float(elem.get("y2", 0))
    return f"M{x1},{y1} L{x2},{y2}"


def get_fill_color(elem):
    """Get fill color from element, defaulting to black."""
    fill = (elem.get("fill") or "").lower()
    if fill == "none" or fill == "transparent":
        return None
    if fill and fill != "#000000":
        return "#FF" + fill[1:]
    return "#FF000000"


def extract_paths(elem, result):
    """Recursively extract path data from SVG elements."""
    tag = elem.tag.split("}")[-1] if "}" in elem.tag else elem.tag

    fill = get_fill_color(elem)

    if tag == "path":
        if fill is not None:
            d = elem.get("d", "")
            if d:
                result.append((fill, d))
    elif tag == "rect":
        if fill is not None:
            d = rect_to_path(elem)
            if d:
                result.append((fill, d))
    elif tag == "polygon":
        if fill is not None:
            d = polygon_to_path(elem)
            if d:
                result.append((fill, d))
    elif tag == "circle":
        if fill is not None:
            d = circle_to_path(elem)
            if d:
                result.append((fill, d))
    elif tag == "line":
        if fill is not None:
            d = line_to_path(elem)
            if d:
                result.append((fill, d))

    for child in elem:
        extract_paths(child, result)


def svg_to_vector_drawable(svg_content, auto_mirror=False):
    """Convert SVG content to Android VectorDrawable XML string."""
    try:
        root = ET.fromstring(svg_content)
    except ET.ParseError as e:
        print(f"  ERROR parsing SVG: {e}")
        return None

    viewBox = root.get("viewBox", "0 0 24 24")
    parts = viewBox.split()
    vw, vh = ("24", "24")
    if len(parts) == 4:
        vw, vh = parts[2], parts[3]

    path_data = []
    extract_paths(root, path_data)

    if not path_data:
        print(f"  WARNING: No paths found in SVG")
        return None

    paths = []
    for fill, d in path_data:
        paths.append(f'    <path\n        android:fillColor="{fill}"\n        android:pathData="{d}" />')

    auto_mirror_attr = '\n    android:autoMirrored="true"' if auto_mirror else ""

    return f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="{vw}"
    android:viewportHeight="{vh}"{auto_mirror_attr}>
{chr(10).join(paths)}
</vector>
"""


def generate_icon(compose_name, category, icon_name, style, auto_mirror=False):
    """Generate a single VectorDrawable XML file."""
    output_name = f"ic_{icon_name}"
    if style == "materialiconsoutlined":
        output_name += "_outlined"
    if auto_mirror:
        output_name += "_mirrored"

    output_path = OUTPUT_DIR / f"{output_name}.xml"

    svg = download_svg(category, icon_name, style)
    if not svg:
        return False

    vd = svg_to_vector_drawable(svg, auto_mirror)
    if not vd:
        return False

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    output_path.write_text(vd, encoding="utf-8")
    print(f"  OK: {output_name}.xml")
    return True


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    success = 0
    failed = 0

    for compose_name, (category, icon_name) in ICON_MAP.items():
        if compose_name.endswith("Outlined"):
            style = "materialiconsoutlined"
            auto_mirror = compose_name in ("ChatOutlined", "NoteAddOutlined")
        elif compose_name in ("ArrowBack", "KeyboardReturn"):
            style = "materialicons"
            auto_mirror = True
        elif compose_name.endswith("Mirrored"):
            style = "materialicons"
            auto_mirror = True
        else:
            style = "materialicons"
            auto_mirror = False

        print(f"Processing {compose_name} ({style})...")
        if generate_icon(compose_name, category, icon_name, style, auto_mirror):
            success += 1
        else:
            failed += 1

    print(f"\nDone: {success} succeeded, {failed} failed")


if __name__ == "__main__":
    main()