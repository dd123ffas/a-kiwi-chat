package kim.biryeong.textcenterer.mixin.client;

import kim.biryeong.textcenterer.chat.ChatScale;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatComponent.class)
abstract class ChatScaleMixin {
    // Rendering, wrapping and click mapping all go through these two methods.
    @Inject(method = "getScale", at = @At("RETURN"), cancellable = true)
    private void textCenterer$chatGuiScale(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValueD() * ChatScale.factor());
    }

    @Inject(method = "getWidth()I", at = @At("RETURN"), cancellable = true)
    private void textCenterer$chatGuiWidth(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.min((int)(cir.getReturnValueI() * ChatScale.factor()), Math.max(40, ChatScale.maxWidth())));
    }

    // Fit the window without touching the chat options, so a small window during startup cannot overwrite them.
    @Inject(method = "getHeight()I", at = @At("RETURN"), cancellable = true)
    private void textCenterer$chatGuiHeight(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.min(cir.getReturnValueI(), Math.max(20, ChatScale.maxHeight())));
    }
}
