package kim.biryeong.textcenterer.mixin.client;

import kim.biryeong.textcenterer.chat.BuiltinChatHeads;
import kim.biryeong.textcenterer.chat.CenteredChatGeometry;
import kim.biryeong.textcenterer.chat.ChatPositionBounds;
import kim.biryeong.textcenterer.chat.ChatScale;
import kim.biryeong.textcenterer.chat.TextCentererConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class ChatGuiScaleMixin {
    // Runs on every resize: fullscreen -> windowed shrinks the window while the GUI scale can stay the same.
    @Inject(method = "resizeGui", at = @At("RETURN"))
    private void textCenterer$fitAfterScaleChange(CallbackInfo ci) {
        Minecraft client = (Minecraft)(Object)this;
        if (client.gui.hud == null) return;
        // Fit from the user's position, not from the previous fit.
        int userX = TextCentererConfig.userChatXOffset(), userY = TextCentererConfig.userChatYOffset();
        TextCentererConfig.previewChatXOffset(userX);
        TextCentererConfig.previewChatYOffset(userY);
        ChatComponent chat = client.gui.hud.getChat();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        double scale = ChatScale.scale();
        double spacing = client.options.chatLineSpacing().get();
        int lineHeight = (int)(client.font.lineHeight * (spacing + 1));
        int textOffset = (int)Math.round(-8 * (spacing + 1) + 4 * spacing);
        double upperPadding = ChatScale.upperPadding(lineHeight, textOffset);
        double lowerPadding = ChatScale.lowerPadding(textOffset);
        // Width and height are limited at render time (ChatScaleMixin); never write the options here.
        chat.rescaleChat();
        int textWidth = (int)Math.ceil(ChatScale.width() / scale);
        var geometry = CenteredChatGeometry.forLine(width, height, scale, 0, 0, 0, textWidth, BuiltinChatHeads.width());
        // Include heads, background padding and both focused/unfocused heights.
        double left = (geometry.backgroundMinX() - BuiltinChatHeads.width() + 4) * scale;
        double right = (geometry.backgroundMaxX() + 4) * scale;
        if (!TextCentererConfig.responsiveBackground()) {
            var background = CenteredChatGeometry.forLine(width, height, scale, 0, 0, 0, textWidth);
            left = Math.min(left, (background.backgroundMinX() + 4) * scale);
            right = Math.max(right, (background.backgroundMaxX() + 4) * scale);
        }
        double bottom = height - TextCentererConfig.chatYOffset();
        double chatHeight = Math.max(ChatScale.height(client.options.chatHeightFocused().get()),
            ChatScale.height(client.options.chatHeightUnfocused().get()));
        double top = bottom - (chatHeight + upperPadding + 1) * scale;
        double lower = bottom + lowerPadding * scale;
        int dx = ChatPositionBounds.correction(left, right, width);
        int dy = ChatPositionBounds.correction(top, lower, height);
        TextCentererConfig.fitChatOffset(userX, userY, userX + dx, userY - dy);
    }
}
