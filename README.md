# A kiwi chat 1.9.9 for Minecraft 26.3

Fabric client mod, ported from the latest built Minecraft 1.21.8 release,
`text-centerer-1.9.3-mc1.21.8.jar` (2026-10-11).

Requires Minecraft **26.3**, **Java 25**, and **Fabric Loader 0.19.5 or newer**.
Mod Menu **21.0.0** is optional and provides the settings entry point.
The minimal Fabric API base/resource-loader modules are bundled so translations
load without a separate Fabric API installation. Cloth Config is not required.
Player heads are built in; a separate Chat Heads mod is not required.

Text Y offset in Layout moves only the rendered text (including link and selection hit areas),
leaving backgrounds and heads fixed. Range: -32 to +32 px; default/reset: 0.
Positive moves text down, negative moves it up.

Chat window height can also be adjusted in Layout settings.
Values are measured from the screen bottom: positive moves up, negative moves down.
The default/reset value is 77 px; changes are saved when closing settings.

Features preserved from 1.9.3:

- Left/center/right chat alignment, X/Y offsets, responsive or fixed backgrounds, clickable links.
- Standalone player heads with UUID/heuristic/both detection, size and Y offset.
- Repeated-message stacking, timestamps, persistent/searchable chat history.
- Whole-line click and Unicode-safe partial/multiline drag selection; Ctrl+C copy.
- Ctrl+F archive search, preserving the draft when returning to chat.
- Warm dark settings/search, draggable scrollbars, animated buttons.
- Unified live layout preview, Shift first-movement axis locking, integer sliders,
  independent inline resets and save on close.
- Chat-screen Layout button, unsent-draft preservation, overlapping-panel fade.
- Offscreen dragging, negative vertical offsets, automatic fit after GUI-scale changes.
- Korean, English, Simplified Chinese and Traditional Chinese translations.

The original project and jars remain in place. Existing `config/text-centerer.properties`
and `config/text-centerer-history.bin` retain their names and formats. Install only
one Text Centerer jar per game instance. To return to 1.21.8, use the original jar
in a 1.21.8 instance.

## Build and verification

Run `./build.ps1` with PowerShell. It uses the sibling project's bundled JDK 25
when available, or your `JAVA_HOME`. Gradle 9.6.0, non-remapping Fabric Loom 1.17.20.

Output: `build/libs/a-kiwi-chat-1.9.9+mc26.3.jar`.

`build` runs geometry, selection, archive, stacking, sender detection and scrollbar
and position/axis-lock regression checks. The dedicated smoke mod is excluded from the distributable.
Run `./build.ps1 runClient -PsmokeTest` for real-client verification; it writes
`build/position-smoke-run/smoke-result.txt` and exits automatically.

See `VERIFICATION.md` for completed checks and limitations.

Selected chat copied with Ctrl+C includes Minecraft § color codes (16 standard colors and §x RGB). Uncolored text stays plain. Old archive entries contain plain text only.

## Credits

Based on Text Centerer by frosti (MIT). Download the jar from the Releases page and put it in your `mods` folder.
