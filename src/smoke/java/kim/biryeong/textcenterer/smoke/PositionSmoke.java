package kim.biryeong.textcenterer.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import kim.biryeong.textcenterer.TextCentererConfigScreen;
import kim.biryeong.textcenterer.chat.TextCentererConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public final class PositionSmoke {
    public static void check(Minecraft client) throws Exception {
        client.options.chatWidth().set(0.5);
        client.options.chatHeightFocused().set(0.2);
        client.options.chatHeightUnfocused().set(0.2);
        client.options.chatScale().set(0.75);
        for (var alignment : TextCentererConfig.TextAlignment.values()) {
            TextCentererConfig.previewTextAlignment(alignment);
            for (int gui : new int[]{1, 2, 3, 1}) {
                client.options.guiScale().set(gui == 1 ? 2 : 1);
                client.resizeGui();
                for (int sign : new int[]{-1, 1}) {
                    TextCentererConfig.previewChatXOffset(sign * 2000);
                    TextCentererConfig.previewChatYOffset(sign * 2000);
                    client.options.guiScale().set(gui);
                    client.resizeGui();
                    int w = client.getWindow().getGuiScaledWidth(), h = client.getWindow().getGuiScaledHeight();
                    double anchor = w / 2.0 + TextCentererConfig.chatXOffset();
                    require(anchor >= 0 && anchor <= w, "X must return onscreen");
                    require(TextCentererConfig.chatYOffset() >= 0 && TextCentererConfig.chatYOffset() <= h, "Y must return onscreen");
                    double chatScale = client.options.chatScale().get();
                    int lineWidth = (int)Math.ceil(net.minecraft.client.gui.components.ChatComponent.getWidth(client.options.chatWidth().get()) / chatScale);
                    int heads = kim.biryeong.textcenterer.chat.BuiltinChatHeads.width();
                    var bounds = kim.biryeong.textcenterer.chat.CenteredChatGeometry.forLine(w, h, chatScale, 0, 0, 0, lineWidth, heads);
                    require((bounds.backgroundMinX() - heads + 4) * chatScale >= 0, "full left edge fits");
                    require((bounds.backgroundMaxX() + 4) * chatScale <= w, "full right edge fits");
                    double bottom = Math.floor((h - TextCentererConfig.chatYOffset()) / chatScale) * chatScale;
                    int lines = net.minecraft.client.gui.components.ChatComponent.getHeight(0.2) / 9;
                    require(bottom - lines * 9 * chatScale >= 0 && bottom <= h, "full vertical extent fits");
                    int x = TextCentererConfig.chatXOffset(), y = TextCentererConfig.chatYOffset();
                    client.resizeGui();
                    require(x == TextCentererConfig.chatXOffset() && y == TextCentererConfig.chatYOffset(), "stable scale");
                    client.options.guiScale().set(gui == 1 ? 2 : 1);
                    client.resizeGui();
                }
            }
        }
        client.options.guiScale().set(1);
        client.resizeGui();
        client.options.chatWidth().set(1.0);
        client.options.chatHeightFocused().set(1.0);
        client.options.chatHeightUnfocused().set(1.0);
        client.options.chatScale().set(1.0);
        TextCentererConfig.previewHeadSize(32);
        TextCentererConfig.previewHeadYOffset(32);
        TextCentererConfig.previewTextAlignment(TextCentererConfig.TextAlignment.CENTER);
        TextCentererConfig.previewChatXOffset(2000);
        TextCentererConfig.previewChatYOffset(-2000);
        client.options.guiScale().set(3);
        client.resizeGui();
        require(client.getWindow().getGuiScale() == 3, "exercise actual GUI scale 3");
        int viewportWidth = client.getWindow().getGuiScaledWidth();
        int viewportHeight = client.getWindow().getGuiScaledHeight();
        int maxWidth = net.minecraft.client.gui.components.ChatComponent.getWidth(client.options.chatWidth().get());
        int headWidth = kim.biryeong.textcenterer.chat.BuiltinChatHeads.width();
        var fitted = kim.biryeong.textcenterer.chat.CenteredChatGeometry.forLine(viewportWidth, viewportHeight, 1, 0, 0, 0, maxWidth, headWidth);
        require(fitted.backgroundMinX() - headWidth + 4 >= 0 && fitted.backgroundMaxX() + 4 <= viewportWidth, "oversized chat fits horizontally");
        int fittedBottom = viewportHeight - TextCentererConfig.chatYOffset();
        int fittedHeight = net.minecraft.client.gui.components.ChatComponent.getHeight(client.options.chatHeightFocused().get());
        require(fittedBottom - fittedHeight >= 0 && fittedBottom + 56 <= viewportHeight, "oversized chat fits vertically with heads");
        TextCentererConfig.previewHeadSize(8);
        TextCentererConfig.previewHeadYOffset(0);
        client.options.guiScale().set(1);
        client.resizeGui();
        TextCentererConfig.previewChatXOffset(0);
        TextCentererConfig.previewChatYOffset(100);
        var screen = new TextCentererConfigScreen(client.gui.screen(), true);
        client.setScreenAndShow(screen);
        double x = screen.width - 40, y = screen.height - 70;
        set(screen, "previewLeft", (int)x - 10);
        set(screen, "previewRight", (int)x + 10);
        set(screen, "previewTop", (int)y - 10);
        set(screen, "previewBottom", (int)y + 10);
        require(screen.mouseClicked(mouse(x, y, 0), false), "start actual screen drag");
        int modifiers = com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT;
        screen.mouseDragged(mouse(x += 20, y += 1, modifiers), 20, 1);
        require(TextCentererConfig.chatXOffset() == 20 && TextCentererConfig.chatYOffset() == 100, "horizontal first");
        screen.mouseDragged(mouse(x, y += 30, modifiers), 0, 30);
        require(TextCentererConfig.chatYOffset() == 100, "horizontal remains locked");
        modifiers = 0;
        screen.mouseDragged(mouse(x += 1, y += 1, modifiers), 1, 1);
        require(TextCentererConfig.chatXOffset() == 21 && TextCentererConfig.chatYOffset() == 99, "release without jump");
        modifiers = com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT;
        screen.mouseDragged(mouse(x += 1, y += 20, modifiers), 1, 20);
        require(TextCentererConfig.chatXOffset() == 21 && TextCentererConfig.chatYOffset() == 79, "vertical after repress");
        screen.mouseDragged(mouse(x += 30, y, modifiers), 30, 0);
        require(TextCentererConfig.chatXOffset() == 21, "vertical remains locked");
        modifiers = 0;
        screen.mouseDragged(mouse(x += 2000, y += 2000, modifiers), 2000, 2000);
        require(TextCentererConfig.chatXOffset() == 2021 && TextCentererConfig.chatYOffset() == -1921, "offscreen right/bottom");
        screen.mouseDragged(mouse(x -= 6000, y -= 6000, modifiers), -6000, -6000);
        require(TextCentererConfig.chatXOffset() == -3979 && TextCentererConfig.chatYOffset() == 4079, "offscreen left/top");
        screen.mouseDragged(mouse(x += 6000, y += 6000, modifiers), 6000, 6000);
        screen.mouseReleased(mouse(x, y, 0));
        screen.onClose();
        client.resizeGui();
        require(TextCentererConfig.chatYOffset() == -1921, "same scale preserves offscreen placement");
        Properties saved = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("config/text-centerer.properties"))) { saved.load(reader); }
        require(saved.getProperty("chat_y_offset").equals("-1921"), "negative offset saved");
        var reload = TextCentererConfig.class.getDeclaredMethod("loadChatYOffset");
        reload.setAccessible(true);
        require((int)reload.invoke(null) == -1921, "negative offset reloads");
    }

    private static net.minecraft.client.input.MouseButtonEvent mouse(double x, double y, int modifiers) {
        return new net.minecraft.client.input.MouseButtonEvent(x, y,
            new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, modifiers));
    }

    private static void set(Object target, String name, int value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
