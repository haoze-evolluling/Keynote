#!/usr/bin/env python3
"""
Replace Material Design Icons library usage with static drawable resources.
Updates all Kotlin files to use vectorResource() instead of Icons.Default/Outlined/AutoMirrored.
"""

import re
import os
from pathlib import Path

KOTLIN_SRC = Path(__file__).parent.parent / "app" / "src" / "main" / "java" / "com" / "haoze" / "keynote"

# Mapping: PascalCase icon name -> snake_case drawable name
FILLED_MAP = {
    "AccountBalance": "account_balance",
    "Add": "add",
    "Archive": "archive",
    "ArrowDropDown": "arrow_drop_down",
    "Assessment": "assessment",
    "AutoAwesome": "auto_awesome",
    "Bookmark": "bookmark",
    "CalendarMonth": "calendar_month",
    "ChatBubble": "chat_bubble",
    "ChatBubbleOutline": "chat_bubble_outline",
    "Check": "check",
    "CheckBox": "check_box",
    "CheckCircle": "check_circle",
    "Clear": "clear",
    "Close": "close",
    "ContentCopy": "content_copy",
    "DarkMode": "dark_mode",
    "Dashboard": "dashboard",
    "DateRange": "date_range",
    "Delete": "delete",
    "DeleteForever": "delete_forever",
    "Description": "description",
    "Done": "done",
    "Download": "download",
    "Edit": "edit",
    "Event": "event",
    "ExpandMore": "expand_more",
    "FileDownload": "file_download",
    "FitnessCenter": "fitness_center",
    "Info": "info",
    "KeyboardArrowRight": "keyboard_arrow_right",
    "Label": "label",
    "LightMode": "light_mode",
    "Lightbulb": "lightbulb",
    "Link": "link",
    "LinkOff": "link_off",
    "LocalLibrary": "local_library",
    "Menu": "menu",
    "MoreVert": "more_vert",
    "People": "people",
    "Psychology": "psychology",
    "PushPin": "push_pin",
    "RadioButtonUnchecked": "radio_button_unchecked",
    "Receipt": "receipt",
    "Restore": "restore",
    "Save": "save",
    "Search": "search",
    "Settings": "settings",
    "SettingsBrightness": "settings_brightness",
    "Share": "share",
    "Undo": "undo",
    "Visibility": "visibility",
    "VisibilityOff": "visibility_off",
    "TextFields": "text_fields",
}

OUTLINED_MAP = {
    "AutoAwesome": "auto_awesome_outlined",
    "CalendarMonth": "calendar_month_outlined",
    "CheckCircle": "check_circle_outlined",
    "Delete": "delete_outlined",
    "Edit": "edit_outlined",
    "History": "history_outlined",
    "Menu": "menu_outlined",
    "Person": "person_outlined",
    "Psychology": "psychology_outlined",
    "Receipt": "receipt_outlined",
    "Refresh": "refresh_outlined",
    "Search": "search_outlined",
}

AUTO_MIRRORED_FILLED_MAP = {
    "ArrowBack": "arrow_back_mirrored",
    "KeyboardArrowRight": "keyboard_arrow_right_mirrored",
    "KeyboardReturn": "keyboard_return_mirrored",
    "Label": "label_mirrored",
}

AUTO_MIRRORED_OUTLINED_MAP = {
    "Chat": "chat_outlined_mirrored",
    "NoteAdd": "note_add_outlined_mirrored",
}


def replace_icons_in_file(filepath):
    content = filepath.read_text(encoding="utf-8")
    original = content
    has_icon_usage = False

    # Sort by key length descending to avoid partial replacements
    # (e.g., "Check" must not replace before "CheckCircle")
    def replace_group(icon_map, prefix):
        nonlocal content, has_icon_usage
        sorted_items = sorted(icon_map.items(), key=lambda x: len(x[0]), reverse=True)
        for pascal, snake in sorted_items:
            pattern = f"{prefix}.{pascal}"
            if pattern in content:
                has_icon_usage = True
                content = content.replace(pattern, f"painterResource(R.drawable.ic_{snake})")

    replace_group(FILLED_MAP, "Icons.Default")
    replace_group(OUTLINED_MAP, "Icons.Outlined")
    replace_group(AUTO_MIRRORED_FILLED_MAP, "Icons.AutoMirrored.Filled")
    replace_group(AUTO_MIRRORED_OUTLINED_MAP, "Icons.AutoMirrored.Outlined")

    if not has_icon_usage:
        return

    # Remove old icon imports
    lines = content.split("\n")
    new_lines = []
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("import androidx.compose.material.icons.Icons"):
            continue
        if stripped.startswith("import androidx.compose.material.icons.filled."):
            continue
        if stripped.startswith("import androidx.compose.material.icons.outlined."):
            continue
        if stripped.startswith("import androidx.compose.material.icons.automirrored."):
            continue
        new_lines.append(line)
    content = "\n".join(new_lines)

    # Add vectorResource and R imports if not already present
    lines = content.split("\n")

    imports_to_add = []
    if "import androidx.compose.ui.res.painterResource" not in content:
        imports_to_add.append("import androidx.compose.ui.res.painterResource")
    if "import com.haoze.keynote.R" not in content:
        imports_to_add.append("import com.haoze.keynote.R")

    if imports_to_add:
        insert_idx = 0
        for i, line in enumerate(lines):
            if line.strip().startswith("import "):
                insert_idx = i + 1
        if insert_idx > 0:
            for imp in reversed(imports_to_add):
                lines.insert(insert_idx, imp)
        content = "\n".join(lines)

    if content != original:
        filepath.write_text(content, encoding="utf-8")
        print(f"  Updated: {filepath.name}")


def main():
    for root, dirs, files in os.walk(KOTLIN_SRC):
        for f in files:
            if f.endswith(".kt"):
                replace_icons_in_file(Path(root) / f)


if __name__ == "__main__":
    main()