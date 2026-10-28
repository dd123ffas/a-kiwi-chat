## 1.9.6+mc26.3

- Add a separate text Y offset to Layout settings, with live preview and reset.
- Move rendered text, clickable links and selection together while preserving background/head positions.
- Rename the existing vertical window control to distinguish it from the new text offset.

## 1.9.5+mc26.3

- Rename the display name and distributable to A kiwi chat.
- Keep the chat Y position slider and reset button exclusively in Layout settings.
- Explain vertical position direction in all four locales.

# 1.9.4+mc26.3

- Rename the mod, settings and distributable to Kiwi Chat; preserve existing mod ID and configuration/history paths.
- Copy rendered chat colors with Minecraft § codes, including RGB colors and Unicode-safe partial/multiline selection.
- Existing plain-text archived search results remain plain text.

# 1.9.3+mc26.3

- Port the last built 1.21.8 release, 1.9.3 (2026-10-11), preserving all existing configuration keys and chat archive format.
- Restore left/center/right alignment across text, heads, fixed/responsive backgrounds, selection and clickable text.
- Restore chat-screen Layout entry and draft return; panel/control/scrollbar fade near the dragged preview.
- Restore free offscreen positioning, negative Y persistence, Shift first-movement axis locking and automatic fitting after GUI scale changes.
- Bundle Fabric API base and resource loader to make all four translations available without a separate API jar.
- Extend the real-client smoke suite and use an isolated build/position-smoke-run directory.

# 1.8.9+mc26.3

- Port the complete 1.8.9 Minecraft 1.21.8 feature set to Minecraft 26.3.
- Move from Yarn/remapping to unobfuscated official game names and Java 25.
- Adapt chat rendering and clickable-text collection to the 26.3 graphics API.
- Port standalone heads, sender metadata, history and repeated-message handling.
- Adapt settings, search, slider and selection input to SDL-backed input events;
  use Minecraft input constants for mouse buttons and keyboard shortcuts.
- Keep configuration/history data compatible with the 1.21.8 build.
