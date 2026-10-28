package kim.biryeong.textcenterer.mixin.client;

import java.util.List;
import kim.biryeong.textcenterer.chat.CenteredGraphicsAccess;
import kim.biryeong.textcenterer.chat.TextCentererConfig;
import kim.biryeong.textcenterer.chat.BuiltinChatHeads;
import kim.biryeong.textcenterer.chat.ChatSelectionRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow private Minecraft minecraft;
    @Shadow private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @org.spongepowered.asm.mixin.Unique private GuiGraphicsExtractor textCenterer$graphics;

    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at=@At("HEAD"))
    private void textCenterer$beginFrame(GuiGraphicsExtractor graphics, Font font, int height, int mouseX, int mouseY,
            ChatComponent.DisplayMode mode, boolean hovered, CallbackInfo ci) {
        ChatSelectionRenderer.beginFrame(minecraft.gui.screen() instanceof ChatScreen);
        textCenterer$graphics = graphics;
    }

    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at=@At("RETURN"))
    private void textCenterer$endFrame(GuiGraphicsExtractor graphics, Font font, int height, int mouseX, int mouseY,
            ChatComponent.DisplayMode mode, boolean hovered, CallbackInfo ci) {
        ChatSelectionRenderer.endFrame();
        textCenterer$graphics = null;
    }

    @Inject(method="lambda$extractRenderState$1", at=@At("HEAD"))
    private static void textCenterer$backgroundLine(int bottom, int lineHeight,
            ChatComponent.ChatGraphicsAccess graphics, int chatWidth, float backgroundOpacity,
            GuiMessage.Line line, int index, float opacity, CallbackInfo ci) {
        if (graphics instanceof CenteredGraphicsAccess centered) {
            centered.setBackgroundLine(line.content(), (BuiltinChatHeads.sender(line) == null ? 0 : BuiltinChatHeads.width()));
        }
    }

    @ModifyConstant(method="extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", constant=@Constant(intValue=40))
    private int textCenterer$offset(int original) {
        return TextCentererConfig.chatYOffset();
    }

    @ModifyVariable(method="extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at=@At("HEAD"), argsOnly=true)
    private ChatComponent.ChatGraphicsAccess textCenterer$center(ChatComponent.ChatGraphicsAccess graphics,
            ChatComponent.ChatGraphicsAccess original, int screenHeight, int ticks, ChatComponent.DisplayMode mode) {
        double scale = kim.biryeong.textcenterer.chat.ChatScale.scale();
        int bottom = (int)Math.floor((screenHeight - TextCentererConfig.chatYOffset()) / (float)scale);
        int lineHeight = (int)(minecraft.font.lineHeight * (minecraft.options.chatLineSpacing().get() + 1));
        CenteredGraphicsAccess centered = new CenteredGraphicsAccess(graphics, minecraft.font::width, minecraft.getWindow().getGuiScaledWidth(), scale, y -> {
            int index = (bottom - y) / lineHeight + chatScrollbarPos;
            return index >= 0 && index < trimmedMessages.size()
                ? minecraft.font.width(trimmedMessages.get(index).content()) : 0;
        });
        centered.setFixedBackgroundWidth((int)Math.ceil(kim.biryeong.textcenterer.chat.ChatScale.width() / scale));
        centered.setHeadGraphics(textCenterer$graphics);
        return centered;
    }
}
