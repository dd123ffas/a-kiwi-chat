package kim.biryeong.textcenterer;

import java.util.ArrayList;
import java.util.List;
import kim.biryeong.textcenterer.chat.ChatArchive;
import kim.biryeong.textcenterer.chat.ChatHistory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;

public final class ChatSearchScreen extends Screen {
    private record Row(ChatArchive.Entry entry, List<FormattedCharSequence> lines, int top, int height) {}
    private final String draft;
    private final List<Row> rows = new ArrayList<>();
    private EditBox search;
    private Button copy;
    private String query = "";
    private int scroll, totalHeight, selected = -1;
    private final DragScrollbar scrollbar = new DragScrollbar();
    public ChatSearchScreen(String draft) {
        super(Component.translatable("textcenterer.search.title"));
        this.draft = draft;
    }
    @Override protected void init() {
        search = addRenderableWidget(new EditBox(font, 12, 30, width - 24, 20,
            Component.translatable("textcenterer.search.query")));
        search.setMaxLength(256);
        search.setHint(Component.translatable("textcenterer.search.query"));
        search.setTextColor(ModernTheme.TEXT);
        search.setValue(query);
        search.setResponder(value -> { query = value; rebuild(); });
        copy = addRenderableWidget(ModernTheme.button(Component.translatable("textcenterer.search.copy"), button -> copySelected())
            .bounds(width / 2 - 104, height - 26, 100, 20).build());
        addRenderableWidget(ModernTheme.button(Component.translatable("gui.back"), button -> onClose())
            .bounds(width / 2 + 4, height - 26, 100, 20).build());
        setInitialFocus(search);
        rebuild();
    }
    private void rebuild() {
        scrollbar.reset();
        rows.clear();
        scroll = totalHeight = 0;
        selected = -1;
        for (ChatArchive.Entry entry : ChatHistory.ARCHIVE.search(query)) {
            List<FormattedCharSequence> lines = font.split(Component.literal(entry.text()), Math.max(40, width - 40));
            int rowHeight = 23 + Math.max(1, lines.size()) * 10;
            rows.add(new Row(entry, lines, totalHeight, rowHeight));
            totalHeight += rowHeight;
        }
        copy.active = false;
    }
    private void copySelected() {
        if (selected >= 0 && selected < rows.size()) minecraft.keyboardHandler.setClipboard(
            kim.biryeong.textcenterer.chat.ChatTextSelection.stripFormatting(rows.get(selected).entry.text()));
    }
    @Override public void tick() {
        super.tick();
        ModernTheme.tickButtons(children());
    }
    private void layoutScrollbar() {
        int viewport = Math.max(0, height - 102);
        scrollbar.layout(width - 7, 68, viewport, viewport, totalHeight, scroll);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ModernTheme.BACKGROUND);
        context.centeredText(font, title, width / 2, 9, ModernTheme.TEXT);
        context.text(font, Component.translatable("textcenterer.search.results", rows.size()), 12, 54, ModernTheme.MUTED, true);
        int bottom = height - 34;
        context.enableScissor(12, 68, width - 12, bottom);
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = 68 + row.top - scroll;
            if (y + row.height < 68) continue;
            if (y >= bottom) break;
            boolean hovered = mouseX >= 12 && mouseX < width - 12 && mouseY >= Math.max(68, y)
                && mouseY < Math.min(bottom, y + row.height - 4);
            ModernTheme.card(context, 12, y, width - 24, row.height - 4,
                i == selected ? ModernTheme.HOVER : hovered ? ModernTheme.SURFACE : ModernTheme.PANEL);
            if (i == selected) context.fill(12, y + 4, 14, y + row.height - 8, ModernTheme.ACCENT);
            context.text(font, ChatHistory.date(row.entry), 20, y + 5, ModernTheme.MUTED, true);
            for (int j = 0; j < row.lines.size(); j++) {
                int lineY = y + 16 + j * 10;
                if (lineY + 10 >= 68 && lineY < bottom)
                    context.text(font, row.lines.get(j), 20, lineY, ModernTheme.TEXT, true);
            }
        }
        context.disableScissor();
        layoutScrollbar();
        ModernTheme.scrollbar(context, scrollbar);
        if (rows.isEmpty()) context.centeredText(font,
            Component.translatable("textcenterer.search.empty"), width / 2, 82, ModernTheme.MUTED);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y(); int button = event.button();
        layoutScrollbar();
        if (scrollbar.click(x, y, button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT ? 0 : -1)) {
            scroll = scrollbar.offsetAt(y);
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && x >= 12 && x < width - 12 && y >= 68 && y < height - 34) {
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                if (y + scroll - 68 >= row.top && y + scroll - 68 < row.top + row.height) {
                    selected = i;
                    copy.active = true;
                    setFocused(null);
                    search.setFocused(false);
                    return true;
                }
            }
        }
        return false;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (scrollbar.dragging()) return true;
        scroll = Math.max(0, Math.min(Math.max(0, totalHeight - (height - 102)), scroll - (int)(vertical * 30)));
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double x = event.x(), y = event.y(); int button = event.button();
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && scrollbar.dragging()) {
            scroll = scrollbar.offsetAt(y);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        double x = event.x(), y = event.y(); int button = event.button();
        if (scrollbar.release(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT ? 0 : -1)) return true;
        return super.mouseReleased(event);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_F && event.hasControlDown()) { setFocused(search); search.setFocused(true); return true; }
        if (event.isCopy() && !search.isFocused() && selected >= 0) { copySelected(); return true; }
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreenAndShow(new ChatScreen(draft, false)); }
}
