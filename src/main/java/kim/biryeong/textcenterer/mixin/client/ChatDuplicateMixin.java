package kim.biryeong.textcenterer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayList;
import java.util.List;
import kim.biryeong.textcenterer.chat.RepeatCounter;
import kim.biryeong.textcenterer.chat.ChatHistory;
import kim.biryeong.textcenterer.chat.TextCentererConfig;
import kim.biryeong.textcenterer.chat.ChatHeadAccess;
import kim.biryeong.textcenterer.chat.HeadDetection;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
abstract class ChatDuplicateMixin {
    @Shadow private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow public abstract int getLinesPerPage();
    @Unique private final RepeatCounter<List<Object>> textCenterer$repeats = new RepeatCounter<>();
    @Unique private final List<GuiMessage.Line> textCenterer$lastGroup = new ArrayList<>();

    @WrapMethod(method = "addMessageToDisplayQueue")
    private void textCenterer$collapse(GuiMessage message, Operation<Void> original) {
        boolean intact = !textCenterer$lastGroup.isEmpty()
            && trimmedMessages.size() >= textCenterer$lastGroup.size();
        for (int i = 0; intact && i < textCenterer$lastGroup.size(); i++) {
            intact = trimmedMessages.get(i) == textCenterer$lastGroup.get(i);
        }
        // Keep styles and indicators distinct: equal wording must not merge different links.
        HeadDetection.Result head = ((ChatHeadAccess)(Object)message).textCenterer$head();
        List<Object> key = java.util.Arrays.asList(message.content().copy(), message.tag(), message.source(), head);
        long count = textCenterer$repeats.accept(key, intact && TextCentererConfig.messageStacking());
        int oldScroll = chatScrollbarPos;
        int removed = count > 1 ? textCenterer$lastGroup.size() : 0;
        if (removed > 0) trimmedMessages.subList(0, removed).clear();
        GuiMessage.Line previousTop = trimmedMessages.isEmpty() ? null : trimmedMessages.get(0);
        GuiMessage displayed = message;
        if (count > 1) {
            // Use a neutral parent so the suffix inherits neither links nor the original color.
            Component content = Component.empty().append(message.content().copy()).append(
                Component.literal(" ×" + count).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            displayed = new GuiMessage(message.addedTime(), content, message.signature(), message.source(), message.tag());
        }
        displayed = new GuiMessage(displayed.addedTime(), ChatHistory.timestamp(message, displayed.content()), displayed.signature(), displayed.source(), displayed.tag());
        original.call(displayed);
        textCenterer$lastGroup.clear();
        for (GuiMessage.Line line : trimmedMessages) {
            if (line == previousTop) break;
            textCenterer$lastGroup.add(line);
        }
        // Visible lines are stored bottom-first. Attach the head only to the first visual row.
        if (!textCenterer$lastGroup.isEmpty()) {
            ((ChatHeadAccess)(Object)textCenterer$lastGroup.get(textCenterer$lastGroup.size() - 1)).textCenterer$head(head);
        }
        if (removed > 0 && oldScroll > 0) {
            chatScrollbarPos = Math.max(0, Math.min(trimmedMessages.size() - getLinesPerPage(),
                oldScroll + textCenterer$lastGroup.size() - removed));
        }
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void textCenterer$resetForRebuild(CallbackInfo ci) {
        textCenterer$repeats.reset();
        textCenterer$lastGroup.clear();
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void textCenterer$resetForClear(boolean clearHistory, CallbackInfo ci) {
        textCenterer$repeats.reset();
        textCenterer$lastGroup.clear();
    }
}
