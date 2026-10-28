package kim.biryeong.textcenterer.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;

/** Chat-only GUI scale: keeps the chat's on-screen size independent of the video GUI scale. */
public final class ChatScale {
    private ChatScale() {}

    /** Ratio between the chat GUI scale and the window GUI scale; 1 when following the video setting. */
    public static double factor() {
        int chatGuiScale = TextCentererConfig.chatGuiScale();
        if (chatGuiScale <= 0) return 1.0;
        int windowScale = Minecraft.getInstance().getWindow().getGuiScale();
        return windowScale <= 0 ? 1.0 : chatGuiScale / (double)windowScale;
    }

    public static double scale() {
        return Minecraft.getInstance().options.chatScale().get() * factor();
    }

    /** Chat width in GUI pixels, limited to the window at render time; the option itself is never changed. */
    public static int width() {
        int width = (int)(ChatComponent.getWidth(Minecraft.getInstance().options.chatWidth().get()) * factor());
        return Math.min(width, Math.max(40, maxWidth()));
    }

    /** Widest chat that fits next to the heads, in GUI pixels. */
    public static int maxWidth() {
        Minecraft client = Minecraft.getInstance();
        return (int)Math.floor(client.getWindow().getGuiScaledWidth() - (BuiltinChatHeads.width() + 10) * scale() - 2);
    }

    /** Tallest chat that fits the window with head padding, in chat units. */
    public static int maxHeight() {
        Minecraft client = Minecraft.getInstance();
        double spacing = client.options.chatLineSpacing().get();
        int lineHeight = (int)(client.font.lineHeight * (spacing + 1));
        int textOffset = (int)Math.round(-8 * (spacing + 1) + 4 * spacing);
        return (int)Math.floor(client.getWindow().getGuiScaledHeight() / scale()
            - upperPadding(lineHeight, textOffset) - lowerPadding(textOffset) - 2);
    }

    public static double upperPadding(int lineHeight, int textOffset) {
        return Math.max(0, -lineHeight - textOffset - TextCentererConfig.headYOffset());
    }

    public static double lowerPadding(int textOffset) {
        return Math.max(0, textOffset + TextCentererConfig.headYOffset() + TextCentererConfig.headSize());
    }

    /** Chat height for the given option value, limited to the window. */
    public static int height(double option) {
        return Math.min(ChatComponent.getHeight(option), Math.max(20, maxHeight()));
    }

    /** Largest GUI scale the current window supports, as in the video settings. */
    public static int maxGuiScale() {
        Minecraft client = Minecraft.getInstance();
        return Math.max(1, client.getWindow().calculateScale(0, client.isEnforceUnicode()));
    }
}
