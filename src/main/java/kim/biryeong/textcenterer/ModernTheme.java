package kim.biryeong.textcenterer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Shared, texture-free styling for the client screens. */
final class ModernTheme {
    static final int BACKGROUND = 0xF51C1917;
    static final int PANEL = 0xF527221F;
    static final int SURFACE = 0xFF362E29;
    static final int HOVER = 0xFF4A3D33;
    static final int ACCENT = 0xFFE4B780;
    static final int TEXT = 0xFFF3EADF;
    static final int MUTED = 0xFFBFAE9E;

    private ModernTheme() {}

    static void tickButtons(java.util.List<? extends GuiEventListener> children) {
        // A button action can rebuild the screen's child list.
        for (GuiEventListener child : java.util.List.copyOf(children)) {
            if (child instanceof AnimatedButton button && button.tickAnimation()) break;
        }
    }

    static void scrollbar(GuiGraphicsExtractor context, DragScrollbar bar) {
        scrollbar(context, bar, 1.0f);
    }

    static int opacity(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.round((color >>> 24) * alpha) << 24);
    }

    static void scrollbar(GuiGraphicsExtractor context, DragScrollbar bar, float alpha) {
        if (!bar.visible()) return;
        context.fill(bar.left(), bar.top(), bar.left() + 2, bar.bottom(), opacity(SURFACE, alpha));
        context.fill(bar.left() - (bar.dragging() ? 1 : 0), bar.thumbTop(),
            bar.left() + (bar.dragging() ? 3 : 2), bar.thumbBottom(), opacity(bar.dragging() ? TEXT : ACCENT, alpha));
    }

    static void card(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        if (width < 4 || height < 4) return;
        context.fill(x + 2, y, x + width - 2, y + height, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
    }

    static Button.Builder button(Component message, Button.OnPress action) {
        return new Button.Builder(message, action) {
            @Override public Button build() {
                Button bounds = super.build();
                return new AnimatedButton(bounds, message, action);
            }
        };
    }

    private static final class AnimatedButton extends Button {
        private long pressedAt;
        private boolean pending;
        private InputWithModifiers pressedInput;

        AnimatedButton(Button bounds, Component message, OnPress action) {
            super(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), message, action,
                supplier -> supplier.get());
        }

        @Override public void onPress(InputWithModifiers input) {
            if (!active || pending) return;
            pressedInput = input;
            pressedAt = System.nanoTime();
            pending = true;
        }

        boolean tickAnimation() {
            if (pending && System.nanoTime() - pressedAt >= 100_000_000L) {
                pending = false;
                // Activate after the press is visible, including navigation buttons.
                if (active && visible) super.onPress(pressedInput);
                pressedInput = null;
                return true;
            }
            return false;
        }

        @Override protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            double elapsed = pressedAt == 0 ? 1 : (System.nanoTime() - pressedAt) / 1_000_000.0;
            double pulse = pressedAt == 0 ? 0 : elapsed < 100
                ? ease(elapsed / 100) : 1 - ease((elapsed - 100) / 180);
            float scale = (float)(1 - 0.035 * pulse);
            float cx = getX() + getWidth() / 2.0f;
            float cy = getY() + getHeight() / 2.0f;
            context.pose().pushMatrix();
            context.pose().translate(cx, cy);
            context.pose().scale(scale, scale);
            context.pose().translate(-cx, -cy);
            boolean highlighted = isHovered() || isFocused();
            card(context, getX(), getY(), getWidth(), getHeight(),
                opacity(!active ? PANEL : pulse > 0.05 || highlighted ? HOVER : SURFACE, alpha));
            if (isFocused()) context.fill(getX() + 4, getY() + getHeight() - 2,
                getX() + getWidth() - 4, getY() + getHeight() - 1, opacity(ACCENT, alpha));
            int textColor = !active ? MUTED : highlighted ? ACCENT : TEXT;
            context.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_ONLY).acceptScrollingWithDefaultCenter(
                getMessage().copy().withStyle(style -> style.withColor(textColor & 0xFFFFFF)),
                getX() + 8, getX() + getWidth() - 8, getY(), getY() + getHeight());
            context.pose().popMatrix();
        }

        private static double ease(double value) {
            double t = Math.clamp(value, 0, 1);
            return t * t * (3 - 2 * t);
        }
    }
}
