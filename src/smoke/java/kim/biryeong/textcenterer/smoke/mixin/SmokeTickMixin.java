package kim.biryeong.textcenterer.smoke.mixin;

import kim.biryeong.textcenterer.smoke.ClientSmoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class SmokeTickMixin {
    @Inject(method="tick", at=@At("TAIL"))
    private void smoke$tick(CallbackInfo ci) { ClientSmoke.tick(Minecraft.getInstance()); }
}
