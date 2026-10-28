package kim.biryeong.textcenterer.mixin.client;

import kim.biryeong.textcenterer.chat.ChatHeadAccess;
import kim.biryeong.textcenterer.chat.HeadDetection;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({GuiMessage.class, GuiMessage.Line.class})
abstract class ChatHeadDataMixin implements ChatHeadAccess {
    @Unique private HeadDetection.Result textCenterer$head;
    public HeadDetection.Result textCenterer$head() { return textCenterer$head; }
    public void textCenterer$head(HeadDetection.Result head) { textCenterer$head = head; }
}
