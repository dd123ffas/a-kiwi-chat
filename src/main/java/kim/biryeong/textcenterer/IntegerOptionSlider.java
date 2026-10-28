package kim.biryeong.textcenterer;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;

/** Integer snapping, including exact one-unit keyboard adjustments. */
final class IntegerOptionSlider extends AbstractSliderButton {
    private final int min, max;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final Component label;

    IntegerOptionSlider(int x, int width, Component label, int min, int max, IntSupplier getter, IntConsumer setter) {
        super(x, 0, width, 28, Component.empty(), 0);
        this.min = min;
        this.max = max;
        this.getter = getter;
        this.setter = setter;
        this.label = label;
        sync();
    }

    void sync() {
        value = Math.clamp((getter.getAsInt() - (double)min) / (max - (double)min), 0, 1);
        updateMessage();
    }

    @Override public void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        ModernTheme.card(context, getX(), getY(), getWidth(), getHeight(),
            ModernTheme.opacity(isHovered() || isFocused() ? ModernTheme.HOVER : ModernTheme.SURFACE, alpha));
        var renderer = net.minecraft.client.Minecraft.getInstance().font;
        String number = Integer.toString(getter.getAsInt());
        int numberWidth = renderer.width(number);
        context.text(renderer, renderer.plainSubstrByWidth(label.getString(), Math.max(0, getWidth() - numberWidth - 28)),
            getX() + 8, getY() + 5, ModernTheme.opacity(ModernTheme.TEXT, alpha), false);
        context.text(renderer, number, getX() + getWidth() - numberWidth - 8, getY() + 5, ModernTheme.opacity(ModernTheme.ACCENT, alpha), false);
        int start = getX() + 4;
        int end = getX() + getWidth() - 4;
        int thumb = start + (int)Math.round(value * (end - start));
        context.fill(start, getY() + 21, end, getY() + 23, ModernTheme.opacity(ModernTheme.BACKGROUND, alpha));
        context.fill(start, getY() + 21, thumb, getY() + 23, ModernTheme.opacity(ModernTheme.ACCENT, alpha));
        ModernTheme.card(context, thumb - 3, getY() + 19, 6, 6, ModernTheme.opacity(isFocused() ? ModernTheme.TEXT : ModernTheme.ACCENT, alpha));
    }

    @Override protected void updateMessage() { setMessage(label.copy().append(": " + getter.getAsInt())); }
    @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() { return label.copy().append(": " + getter.getAsInt()); }
    @Override protected void applyValue() {
        int next = min + (int)Math.round(value * (max - (double)min));
        if (next != getter.getAsInt()) setter.accept(next);
        sync();
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (isFocused() && (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT)) {
            setter.accept(Math.clamp(getter.getAsInt() + (keyCode == InputConstants.KEY_LEFT ? -1 : 1), min, max));
            sync();
            return true;
        }
        return super.keyPressed(event);
    }
}
