package kim.biryeong.textcenterer;

import kim.biryeong.textcenterer.chat.CenteredChatGeometry;
import kim.biryeong.textcenterer.chat.TextCentererConfig;
import kim.biryeong.textcenterer.chat.ChatHistory;
import kim.biryeong.textcenterer.chat.ChatHistoryAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;

public final class TextCentererConfigScreen extends Screen {
    private final Screen parent;
    private final boolean positionPage;
    private final java.util.List<java.util.List<net.minecraft.client.gui.components.AbstractWidget>> rows = new java.util.ArrayList<>();
    private final java.util.List<IntegerOptionSlider> sliders = new java.util.ArrayList<>();
    private int firstRow, visibleRows, panelLeft, panelRight, panelBottom, rowsTop;
    private int initialHistoryLimit;
    private final DragScrollbar scrollbar = new DragScrollbar();

    private boolean dragging;
    private double grabX, grabY;
    private final DragAxisLock axisLock = new DragAxisLock();
    private int previewLeft, previewRight, previewTop, previewBottom;

    public TextCentererConfigScreen(Screen parent) {
        this(parent, false);
    }

    public TextCentererConfigScreen(Screen parent, boolean positionPage) {
        super(Component.translatable(positionPage ? "textcenterer.options.position" : "textcenterer.options.title"));
        this.parent = parent;
        this.positionPage = positionPage;
        this.initialHistoryLimit = TextCentererConfig.historyLimit();
    }

    @Override
    protected void init() {
        rows.clear();
        sliders.clear();
        dragging = false;
        axisLock.reset();
        scrollbar.reset();
        int totalWidth = Math.min(positionPage ? 260 : 300, this.width - 48);
        int columnWidth = totalWidth;
        int left = positionPage && width >= 600 ? 24 : (width - totalWidth) / 2;
        panelLeft = left - 12;
        panelRight = left + totalWidth + 12;
        if (!positionPage) {
            buttonRow(ModernTheme.button(Component.translatable("textcenterer.options.position"), button ->
                minecraft.setScreenAndShow(new TextCentererConfigScreen(this, true)))
                .bounds(left, 0, columnWidth, 28).build());
        } else {
            buttonRow(ModernTheme.button(alignmentLabel(), button -> {
                TextCentererConfig.cycleTextAlignment();
                button.setMessage(alignmentLabel());
            }).bounds(left, 0, columnWidth, 28).build());
            Button chatGuiScale = ModernTheme.button(chatGuiScaleLabel(), button -> {
                int next = TextCentererConfig.chatGuiScale() + 1;
                TextCentererConfig.previewChatGuiScale(next > kim.biryeong.textcenterer.chat.ChatScale.maxGuiScale() ? 0 : next);
                button.setMessage(chatGuiScaleLabel());
                minecraft.gui.hud.getChat().rescaleChat();
            }).bounds(left, 0, columnWidth, 28).build();
            chatGuiScale.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("textcenterer.options.chat_gui_scale.tooltip")));
            buttonRow(chatGuiScale);
            numericRow(left, columnWidth, "head_size", 1, 32, 8,
                TextCentererConfig::headSize, TextCentererConfig::previewHeadSize);
            numericRow(left, columnWidth, "head_y_offset", -32, 32, 0,
                TextCentererConfig::headYOffset, TextCentererConfig::previewHeadYOffset);
            int xRange = Math.max(width, Math.abs(TextCentererConfig.chatXOffset()));
            numericRow(left, columnWidth, "chat_x_offset", -xRange, xRange, 0,
                TextCentererConfig::chatXOffset, TextCentererConfig::previewChatXOffset);
            int yRange = Math.max(height, Math.abs(TextCentererConfig.chatYOffset()));
            numericRow(left, columnWidth, "chat_y_offset", -yRange, yRange, TextCentererConfig.DEFAULT_CHAT_Y_OFFSET,
                TextCentererConfig::chatYOffset, TextCentererConfig::previewChatYOffset);
            numericRow(left, columnWidth, "chat_text_y_offset", -32, 32, 0,
                TextCentererConfig::chatTextYOffset, TextCentererConfig::previewChatTextYOffset);
        }
        if (!positionPage) {
            numericRow(left, columnWidth, "history_limit", 1000, 50000, null,
                TextCentererConfig::historyLimit, TextCentererConfig::previewHistoryLimit);
            buttonRow(ModernTheme.button(backgroundLabel(), button -> {
                TextCentererConfig.previewResponsiveBackground(!TextCentererConfig.responsiveBackground());
                button.setMessage(backgroundLabel());
            }).bounds(left, 0, columnWidth, 28).build());
            buttonRow(ModernTheme.button(optionLabel("timestamps", TextCentererConfig.timestamps()), button -> {
                TextCentererConfig.toggleTimestamps();
                button.setMessage(optionLabel("timestamps", TextCentererConfig.timestamps()));
                ((ChatHistoryAccess)minecraft.gui.hud.getChat()).textCenterer$refreshHistory();
            }).bounds(left, 0, columnWidth, 28).build());
            buttonRow(ModernTheme.button(optionLabel("persist", TextCentererConfig.persistHistory()), button -> {
                TextCentererConfig.togglePersistHistory();
                ChatHistory.configure();
                button.setMessage(optionLabel("persist", TextCentererConfig.persistHistory()));
                ((ChatHistoryAccess)minecraft.gui.hud.getChat()).textCenterer$restoreHistory();
            }).bounds(left, 0, columnWidth, 28).build());
            buttonRow(ModernTheme.button(optionLabel("message_stacking", TextCentererConfig.messageStacking()), button -> {
                TextCentererConfig.toggleMessageStacking();
                button.setMessage(optionLabel("message_stacking", TextCentererConfig.messageStacking()));
                ((ChatHistoryAccess)minecraft.gui.hud.getChat()).textCenterer$refreshHistory();
            }).bounds(left, 0, columnWidth, 28).build());
            buttonRow(ModernTheme.button(headDetectionLabel(), button -> {
                TextCentererConfig.cycleHeadDetectionMode();
                button.setMessage(headDetectionLabel());
            }).bounds(left, 0, columnWidth, 28).build());
        }
        rowsTop = 62;
        visibleRows = Math.max(1, Math.min(rows.size(), (height - rowsTop - 40) / 34));
        panelBottom = rowsTop + visibleRows * 34;
        layoutRows();
        this.addRenderableWidget(ModernTheme.button(Component.translatable("gui.done"), button -> this.onClose())
            .bounds(left, height - 28, totalWidth, 20).build());
    }

    private void buttonRow(Button button) {
        rows.add(java.util.List.of(addRenderableWidget(button)));
    }

    private void numericRow(int left, int width, String key, int min, int max, Integer resetValue,
            java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        Component label = Component.translatable("textcenterer.options.label." + key);
        Component resetLabel = Component.translatable("controls.reset");
        int resetWidth = resetValue == null ? 0 : Math.clamp(font.width(resetLabel) + 16, 44, 72);
        int sliderWidth = resetValue == null ? width : width - resetWidth - 6;
        IntegerOptionSlider slider = addRenderableWidget(new IntegerOptionSlider(left, sliderWidth, label, min, max, getter, setter));
        if ("chat_text_y_offset".equals(key) || "chat_y_offset".equals(key)) {
            slider.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("textcenterer.options." + key + ".tooltip")));
        }
        if (resetValue != null) {
            Button reset = addRenderableWidget(ModernTheme.button(resetLabel, button -> {
                setter.accept(resetValue);
                slider.sync();
            }).bounds(left + sliderWidth + 6, 0, resetWidth, 28).build());
            reset.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                label.copy().append(": " + resetValue)));
            rows.add(java.util.List.of(slider, reset));
        } else {
            rows.add(java.util.List.of(slider));
        }
        sliders.add(slider);
    }

    private void layoutRows() {
        firstRow = Math.clamp(firstRow, 0, Math.max(0, rows.size() - visibleRows));
        for (int i = 0; i < rows.size(); i++) {
            for (var widget : rows.get(i)) {
                widget.setY(rowsTop + (i - firstRow) * 34);
                widget.visible = i >= firstRow && i < firstRow + visibleRows;
                if (!widget.visible && getFocused() == widget) setFocused(null);
            }
        }
    }

    @Override public void tick() {
        super.tick();
        ModernTheme.tickButtons(children());
        if (!minecraft.hasShiftDown()) axisLock.reset();
    }

    private void layoutScrollbar() {
        scrollbar.layout(panelRight - 6, rowsTop, visibleRows * 34, visibleRows, rows.size(), firstRow);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (scrollbar.dragging()) return true;
        if (x >= panelLeft && x <= panelRight + 8 && y >= rowsTop && y < panelBottom) {
            firstRow -= (int)Math.signum(vertical);
            layoutRows();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Keep the world visible so the position can be judged against the HUD.
        if (this.minecraft.level == null) {
            context.fill(0, 0, this.width, this.height, ModernTheme.BACKGROUND);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.extractBackground(context, mouseX, mouseY, delta);
        if (!positionPage && minecraft.level != null) context.fill(0, 0, width, height, 0xA01C1917);
        if (positionPage) renderPreview(context);
        float panelAlpha = positionPage && dragging
            && previewRight >= panelLeft - 16 && previewLeft <= panelRight + 16
            && previewBottom >= rowsTop - 24 && previewTop <= panelBottom + 16 ? 0.35f : 1.0f;
        for (var row : rows) for (var widget : row) widget.setAlpha(panelAlpha);
        ModernTheme.card(context, panelLeft, rowsTop - 8, panelRight - panelLeft, panelBottom - rowsTop + 8,
            ModernTheme.opacity(ModernTheme.PANEL, panelAlpha));
        layoutScrollbar();
        ModernTheme.scrollbar(context, scrollbar, panelAlpha);
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 16, ModernTheme.TEXT);
        context.centeredText(this.font, Component.translatable(positionPage
            ? "textcenterer.options.hint" : "textcenterer.options.slider_hint"), this.width / 2, 35, ModernTheme.MUTED);
    }

    private void renderPreview(GuiGraphicsExtractor context) {
        double scale = kim.biryeong.textcenterer.chat.ChatScale.scale();
        int bottom = (int)Math.floor((this.height - TextCentererConfig.chatYOffset()) / (float)scale);
        double spacing = this.minecraft.options.chatLineSpacing().get();
        int lineHeight = (int)(this.font.lineHeight * (spacing + 1));
        int textOffset = (int)Math.round(-8 * (spacing + 1) + 4 * spacing);
        int headWidth = kim.biryeong.textcenterer.chat.BuiltinChatHeads.width();
        previewLeft = Integer.MAX_VALUE;
        previewRight = Integer.MIN_VALUE;
        previewTop = Integer.MAX_VALUE;
        previewBottom = Integer.MIN_VALUE;
        context.pose().pushMatrix();
        context.pose().scale((float)scale, (float)scale);
        context.pose().translate(4.0f, 0.0f);
        var previewSkin = this.minecraft.player == null
            ? net.minecraft.client.resources.DefaultPlayerSkin.get(new java.util.UUID(0, 0))
            : this.minecraft.player.getSkin();
        for (int row = 0; row < 3; row++) {
            Component preview = Component.translatable("textcenterer.options.preview." + (3 - row));
            int rowBottom = bottom - row * lineHeight;
            CenteredChatGeometry geometry = CenteredChatGeometry.forLine(this.width, this.height, scale,
                lineHeight, rowBottom, 0, this.font.width(preview), headWidth);
            CenteredChatGeometry background = TextCentererConfig.responsiveBackground() ? geometry
                : CenteredChatGeometry.forLine(this.width, this.height, scale, lineHeight, rowBottom, 0,
                    (int)Math.ceil(kim.biryeong.textcenterer.chat.ChatScale.width() / scale));
            int left = Math.min(background.backgroundMinX(), geometry.backgroundMinX() - headWidth);
            int headTop = rowBottom + textOffset + TextCentererConfig.headYOffset();
            previewLeft = Math.min(previewLeft, (int)((left + 4) * scale) - 6);
            previewRight = Math.max(previewRight, (int)((background.backgroundMaxX() + 4) * scale) + 6);
            previewTop = Math.min(previewTop, (int)(Math.min(geometry.topY(), headTop) * scale) - 6);
            previewBottom = Math.max(previewBottom, (int)(Math.max(rowBottom, headTop + TextCentererConfig.headSize()) * scale) + 6);
            context.fill(left, geometry.topY(), background.backgroundMaxX(), rowBottom, 0xC0000000);
            context.text(this.font, preview, geometry.textX(), rowBottom + textOffset + TextCentererConfig.chatTextYOffset(), 0xFFFFFFFF, true);
            net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(context, previewSkin,
                geometry.textX() - headWidth, headTop, TextCentererConfig.headSize());
        }
        context.pose().popMatrix();
        int border = dragging ? ModernTheme.ACCENT : (ModernTheme.ACCENT & 0x00FFFFFF) | 0xAA000000;
        context.fill(previewLeft, previewTop, previewRight, previewTop + 1, border);
        context.fill(previewLeft, previewBottom - 1, previewRight, previewBottom, border);
        context.fill(previewLeft, previewTop, previewLeft + 1, previewBottom, border);
        context.fill(previewRight - 1, previewTop, previewRight, previewBottom, border);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public void removed() {
        TextCentererConfig.setChatYOffset(TextCentererConfig.chatYOffset());
        if (initialHistoryLimit != TextCentererConfig.historyLimit()) {
            ChatHistory.configure();
            ((ChatHistoryAccess)minecraft.gui.hud.getChat()).textCenterer$refreshHistory();
            initialHistoryLimit = TextCentererConfig.historyLimit();
        }
        ChatHistory.save();
    }

    private Component alignmentLabel() {
        return Component.translatable("textcenterer.options.alignment", Component.translatable(
            "textcenterer.options.alignment." + TextCentererConfig.textAlignment().name().toLowerCase(java.util.Locale.ROOT)));
    }

    private Component chatGuiScaleLabel() {
        int value = TextCentererConfig.chatGuiScale();
        return Component.translatable("textcenterer.options.chat_gui_scale", value <= 0
            ? Component.translatable("textcenterer.options.chat_gui_scale.video") : Component.literal(Integer.toString(value)));
    }

    private Component headDetectionLabel() {
        return Component.translatable("textcenterer.options.head_detection", Component.translatable(
            "textcenterer.options.head_detection." + TextCentererConfig.headDetectionMode().name().toLowerCase(java.util.Locale.ROOT)));
    }

    private Component optionLabel(String key, boolean enabled) {
        return Component.translatable("textcenterer.options." + key, Component.translatable(enabled ? "options.on" : "options.off"));
    }

    private Component backgroundLabel() {
        return Component.translatable("textcenterer.options.background", Component.translatable(
            TextCentererConfig.responsiveBackground() ? "options.on" : "options.off"));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y(); int button = event.button();
        layoutScrollbar();
        if (scrollbar.click(x, y, button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT ? 0 : -1)) {
            firstRow = scrollbar.offsetAt(y);
            layoutRows();
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (x >= panelLeft && x <= panelRight && y >= rowsTop - 8 && y < panelBottom) return true;
        if (positionPage && button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && x >= previewLeft && x <= previewRight && y >= previewTop && y <= previewBottom) {
            dragging = true;
            axisLock.reset();
            grabX = x - (width / 2.0 + TextCentererConfig.chatXOffset());
            grabY = y - (height - TextCentererConfig.chatYOffset());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double x = event.x(), y = event.y(); int button = event.button();
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && scrollbar.dragging()) {
            firstRow = scrollbar.offsetAt(y);
            layoutRows();
            return true;
        }
        if (dragging && button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            axisLock.update(event.hasShiftDown(), dx, dy);
            if (axisLock.horizontal()) {
                TextCentererConfig.previewChatXOffset((int)Math.round(x - grabX - width / 2.0));
            } else {
                grabX = x - (width / 2.0 + TextCentererConfig.chatXOffset());
            }
            if (axisLock.vertical()) {
                TextCentererConfig.previewChatYOffset((int)Math.round(height - (y - grabY)));
            } else {
                grabY = y - (height - TextCentererConfig.chatYOffset());
            }
            sliders.forEach(IntegerOptionSlider::sync);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double x = event.x(), y = event.y(); int button = event.button();
        if (scrollbar.release(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT ? 0 : -1)) return true;
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && dragging) { dragging = false; return true; }
        return super.mouseReleased(event);
    }

}
