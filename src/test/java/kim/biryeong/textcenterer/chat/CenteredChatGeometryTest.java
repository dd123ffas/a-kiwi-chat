package kim.biryeong.textcenterer.chat;

/** Standalone regression checks; no Minecraft client or test dependencies needed. */
public final class CenteredChatGeometryTest {
    public static void main(String[] args) {
        TextCentererConfig.previewTextAlignment(TextCentererConfig.TextAlignment.CENTER);
        TextCentererConfig.previewChatXOffset(0);
        int cases = 0;
        int formerlyRejected = 0;
        for (int pixels : new int[]{854, 1280, 1920, 2560, 3840}) {
            for (int guiScale : new int[]{1, 2, 3, 4, 5, 6}) {
                int viewport = (int)Math.ceil((double)pixels / guiScale);
                for (double scale : new double[]{0.25, 0.5, 0.75, 0.9, 1.0}) {
                    for (int width : new int[]{1, 37, 120, 319}) {
                        CenteredChatGeometry geometry = CenteredChatGeometry.forLine(
                            viewport, 480, scale, 9, 400, 0, width);
                        // Reconstruct actual screen positions using the render matrix.
                        double left = (geometry.textX() + 4) * scale;
                        for (int characterX = 0; characterX < width; characterX++) {
                            double mouseX = left + (characterX + 0.5) * scale;
                            double actual = geometry.relativeTextX(mouseX, scale);
                            if ((int)Math.floor(actual) != characterX) {
                                throw new AssertionError("Wrong link character at " + pixels + "/" + guiScale + "/" + scale);
                            }
                            if (mouseX / scale - 4 > Math.floor(320 / scale)) formerlyRejected++;
                        }
                        if (geometry.relativeTextX(left - scale * 0.5, scale) >= 0
                            || geometry.relativeTextX(left + (width + 0.5) * scale, scale) < width) {
                            throw new AssertionError("Outside text accepted");
                        }
                        cases++;
                    }
                }
            }
        }
        if (formerlyRejected == 0) throw new AssertionError("Missing original bug coverage");
        for (int offset : new int[]{-180, 0, 180}) {
            TextCentererConfig.previewChatXOffset(offset);
            for (double scale : new double[]{0.25, 0.75, 1.0}) {
                CenteredChatGeometry geometry = CenteredChatGeometry.forLine(854, 480, scale, 9, 300, 0, 120);
                double left = (geometry.textX() + 4) * scale;
                if (Math.abs(left + 60 * scale - (427 + offset)) > scale)
                    throw new AssertionError("Incorrect dragged center");
                if ((int)Math.floor(geometry.relativeTextX(left + 25.5 * scale, scale)) != 25)
                    throw new AssertionError("Dragged link hit test failed");
            }
        }
        TextCentererConfig.previewChatXOffset(0);
        for (var alignment : TextCentererConfig.TextAlignment.values()) {
            TextCentererConfig.previewTextAlignment(alignment);
            for (int offset : new int[]{-180, 0, 180}) {
                TextCentererConfig.previewChatXOffset(offset);
                for (double scale : new double[]{0.25, 0.75, 1.0}) {
                    for (int width : new int[]{0, 1, 37, 120, 319}) {
                        var geometry = CenteredChatGeometry.forLine(854, 480, scale, 9, 300, 2, width);
                        int anchor = (int)Math.floor((427 + offset) / scale) - 4;
                        int actualAnchor = switch (alignment) {
                            case LEFT -> geometry.textX();
                            case CENTER -> geometry.textX() + width / 2;
                            case RIGHT -> geometry.textX() + width;
                        };
                        if (actualAnchor != anchor) throw new AssertionError("Wrong alignment: " + alignment);
                        for (int headWidth : new int[]{2, 10, 26}) {
                            var headed = CenteredChatGeometry.forLine(854, 480, scale, 9, 300, 2, width, headWidth);
                            if (alignment == TextCentererConfig.TextAlignment.LEFT) {
                                if (headed.textX() - headWidth != anchor || geometry.textX() != anchor)
                                    throw new AssertionError("Headless text must align with the headed line's icon edge");
                                if (headed.backgroundMinX() - headWidth != geometry.backgroundMinX())
                                    throw new AssertionError("Left background edges differ");
                            } else if (headed.textX() != geometry.textX()) {
                                throw new AssertionError("Center/right text anchor changed");
                            }
                            if (alignment == TextCentererConfig.TextAlignment.RIGHT
                                && headed.textX() + width != anchor)
                                throw new AssertionError("Right text edge must ignore head spacing");
                            if (width > 0 && (int)Math.floor(headed.relativeTextX(
                                (headed.textX() + 4.5) * scale, scale)) != 0)
                                throw new AssertionError("Headed link hit test shifted");
                        }
                        if (geometry.backgroundMinX() != geometry.textX() - 4
                            || geometry.backgroundMaxX() != geometry.textX() + width + 4
                            || geometry.topY() != 273 || geometry.bottomY() != 282)
                            throw new AssertionError("Wrong background bounds");
                        for (int x = 0; x < width; x++) {
                            double mouse = (geometry.textX() + 4 + x + 0.5) * scale;
                            if ((int)Math.floor(geometry.relativeTextX(mouse, scale)) != x)
                                throw new AssertionError("Wrong aligned link position");
                        }
                    }
                }
            }
            if (TextCentererConfig.TextAlignment.parse(alignment.name()) != alignment)
                throw new AssertionError("Alignment config round trip failed");
        }
        if (TextCentererConfig.TextAlignment.parse("invalid") != TextCentererConfig.TextAlignment.CENTER
            || TextCentererConfig.TextAlignment.parse(null) != TextCentererConfig.TextAlignment.CENTER)
            throw new AssertionError("Invalid configuration must default to center");
        TextCentererConfig.previewChatXOffset(0);
        TextCentererConfig.previewTextAlignment(TextCentererConfig.TextAlignment.CENTER);
        TextCentererConfig.cycleTextAlignment();
        TextCentererConfig.cycleTextAlignment();
        TextCentererConfig.cycleTextAlignment();
        if (TextCentererConfig.textAlignment() != TextCentererConfig.TextAlignment.CENTER)
            throw new AssertionError("Alignment cycle failed");
        System.out.println("PASS: all alignments and " + cases + " geometry combinations; " + formerlyRejected + " formerly rejected text positions");
    }
}
