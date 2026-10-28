package kim.biryeong.textcenterer.chat;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

/** Applies the same transform to drawn text and vanilla's clickable text collector. */
public final class CenteredGraphicsAccess implements ChatGraphicsAccess {
    private final ChatGraphicsAccess delegate;
    private final ToIntFunction<FormattedCharSequence> textWidth;
    private final int center;
    private final IntUnaryOperator widthAtBottom;
    private int lastTextX;
    private boolean insideLine;
    private int lineHeadWidth;
    private int lineTextWidth;
    private int backgroundWidth = -1;
    private int backgroundHeadWidth;
    private net.minecraft.client.gui.GuiGraphicsExtractor headGraphics;
    private final Matrix3x2f pose = new Matrix3x2f();
    private net.minecraft.client.multiplayer.chat.GuiMessage.Line selectionLine;
    private int fixedBackgroundWidth = -1;

    public CenteredGraphicsAccess(ChatGraphicsAccess delegate, ToIntFunction<FormattedCharSequence> textWidth, int viewportWidth,
            double scale, IntUnaryOperator widthAtBottom) {
        this.delegate = delegate;
        this.textWidth = textWidth;
        this.center = (int)Math.floor((viewportWidth / 2.0 + TextCentererConfig.chatXOffset()) / scale) - 4;
        this.widthAtBottom = widthAtBottom;
    }

    private int textX(int width, int headWidth) {
        return center - switch (TextCentererConfig.textAlignment()) {
            case LEFT -> -headWidth;
            case CENTER -> width / 2;
            case RIGHT -> width;
        };
    }

    @Override
    public void updatePose(Consumer<Matrix3x2f> update) {
        update.accept(pose);
        delegate.updatePose(update);
    }

    @Override
    public void fill(int left, int top, int right, int bottom, int color) {
        if (left == -4) {
            int width = backgroundWidth >= 0 ? backgroundWidth : widthAtBottom.applyAsInt(bottom);
            boolean responsive = TextCentererConfig.responsiveBackground();
            if (!responsive && fixedBackgroundWidth >= 0) width = fixedBackgroundWidth;
            int x = textX(width, responsive ? backgroundHeadWidth : 0);
            delegate.fill(x - 4 - (responsive ? backgroundHeadWidth : 0), top, x + width + 4, bottom, color);
        } else if (left == -2) {
            int shift = textX(right - 4, 0);
            delegate.fill(left + shift, top, right + shift, bottom, color);
        }
        // The old left-aligned scrollbar is intentionally hidden.
    }

    public void setBackgroundLine(FormattedCharSequence message, int extraWidth) {
        backgroundWidth = textWidth.applyAsInt(message);
        backgroundHeadWidth = extraWidth;
    }

    @Override
    public boolean handleMessage(int top, float opacity, FormattedCharSequence message) {
        if (insideLine) {
            ChatSelectionRenderer.draw(delegate, selectionLine, top + TextCentererConfig.chatTextYOffset(), pose);
            boolean hovered = delegate.handleMessage(top + TextCentererConfig.chatTextYOffset(), opacity, message);
            if (headGraphics != null && selectionLine != null) {
                BuiltinChatHeads.extractRenderState(headGraphics, selectionLine, 0, top, opacity);
            }
            return hovered;
        }
        lastTextX = textX(textWidth.applyAsInt(message), 0);
        int offset = lastTextX;
        updatePose(pose -> pose.translate(offset, 0));
        try {
            return delegate.handleMessage(top + TextCentererConfig.chatTextYOffset(), opacity, message);
        } finally {
            updatePose(pose -> pose.translate(-offset, 0));
        }
    }

    public void beginLine(FormattedCharSequence message, int extraWidth) {
        lineHeadWidth = extraWidth;
        lineTextWidth = textWidth.applyAsInt(message);
        lastTextX = textX(lineTextWidth, lineHeadWidth);
        int offset = lastTextX;
        updatePose(pose -> pose.translate(offset, 0));
        insideLine = true;
    }

    public void endLine() {
        int offset = lastTextX;
        updatePose(pose -> pose.translate(-offset, 0));
        insideLine = false;
        selectionLine = null;
    }

    public void setSelectionLine(net.minecraft.client.multiplayer.chat.GuiMessage.Line line) { selectionLine = line; }
    public void setFixedBackgroundWidth(int width) { fixedBackgroundWidth = width; }
    public void setHeadGraphics(net.minecraft.client.gui.GuiGraphicsExtractor graphics) { headGraphics = graphics; }

    @Override
    public void handleTag(int left, int top, int right, int bottom, float opacity, GuiMessageTag tag) {
        int offset = insideLine ? 0 : lastTextX;
        if (insideLine) offset = TextCentererConfig.responsiveBackground() ? -lineHeadWidth
            : fixedBackgroundWidth >= 0 ? textX(fixedBackgroundWidth, 0) - lastTextX : 0;
        delegate.handleTag(left + offset, top, right + offset, bottom, opacity, tag);
    }

    @Override
    public void handleTagIcon(int left, int bottom, boolean forceVisible, GuiMessageTag tag, GuiMessageTag.Icon icon) {
        int offset = insideLine ? 0 : lastTextX;
        if (insideLine && !TextCentererConfig.responsiveBackground() && fixedBackgroundWidth >= 0)
            offset = textX(fixedBackgroundWidth, 0) + fixedBackgroundWidth - lastTextX - lineTextWidth;
        delegate.handleTagIcon(left + offset, bottom, forceVisible, tag, icon);
    }
}
