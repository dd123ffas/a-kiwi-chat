package kim.biryeong.textcenterer.mixin.client;

import kim.biryeong.textcenterer.chat.CenteredGraphicsAccess;
import kim.biryeong.textcenterer.chat.BuiltinChatHeads;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.client.gui.components.ChatComponent$1", priority=1100)
public abstract class ChatLineMixin {
    @Shadow @Final private ChatComponent.ChatGraphicsAccess val$graphics;

    @Inject(method="accept", at=@At("HEAD"))
    private void textCenterer$begin(GuiMessage.Line line, int index, float opacity, CallbackInfo ci) {
        if (val$graphics instanceof CenteredGraphicsAccess centered) {
            centered.setSelectionLine(line);
            centered.beginLine(line.content(), (BuiltinChatHeads.sender(line) == null ? 0 : BuiltinChatHeads.width()));
        }
    }

    @Inject(method="accept", at=@At("RETURN"))
    private void textCenterer$end(GuiMessage.Line line, int index, float opacity, CallbackInfo ci) {
        if (val$graphics instanceof CenteredGraphicsAccess centered) centered.endLine();
    }
}
