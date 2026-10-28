package kim.biryeong.textcenterer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.*;
import kim.biryeong.textcenterer.chat.*;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
abstract class ChatHistoryMixin implements ChatHistoryAccess {
    @Shadow private List<GuiMessage> allMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow private void refreshTrimmedMessages() { throw new AssertionError(); }

    @ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
    private int textCenterer$expandedLimit(int original) { return TextCentererConfig.historyLimit(); }

    @WrapOperation(method = "addMessage",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;logChatMessage(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"))
    private void textCenterer$record(ChatComponent hud, GuiMessage line, Operation<Void> original) {
        BuiltinChatHeads.capture(line);
        ChatHistory.record(line);
        original.call(hud, line);
    }

    @Inject(method = "clearMessages", at = @At("RETURN"))
    private void textCenterer$clearArchive(boolean clearHistory, CallbackInfo ci) {
        ChatHistory.save();
        if (!clearHistory || !TextCentererConfig.persistHistory()) {
            try { ChatHistory.ARCHIVE.clear(); }
            catch (java.io.IOException e) { kim.biryeong.textcenterer.TextCentererClient.LOGGER.error("Cannot clear chat archive", e); }
        }
        ChatHistory.ENTRIES.clear();
    }

    @Inject(method = "deleteMessage", at = @At("HEAD"))
    private void textCenterer$removeArchivedMessage(MessageSignature signature, CallbackInfo ci) {
        for (GuiMessage line : allMessages) {
            if (signature.equals(line.signature())) {
                ChatArchive.Entry entry = ChatHistory.ENTRIES.get(line);
                if (entry != null) ChatHistory.ARCHIVE.remove(entry.id());
            }
        }
    }

    @Override
    public void textCenterer$restoreHistory() {
        if (!TextCentererConfig.persistHistory()) return;
        Set<String> present = new HashSet<>();
        for (GuiMessage line : allMessages) {
            ChatArchive.Entry entry = ChatHistory.ENTRIES.get(line);
            if (entry != null) present.add(entry.id());
        }
        List<ChatArchive.Entry> archive = ChatHistory.ARCHIVE.entries();
        boolean changed = false;
        for (int i = archive.size() - 1; i >= 0; i--) {
            ChatArchive.Entry entry = archive.get(i);
            if (present.add(entry.id())) {
                Component content = Component.empty().append(Component.translatable("textcenterer.chat.archived").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(entry.text()));
                GuiMessage line = new GuiMessage(-1000000, content, null, net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_CLIENT, null);
                ChatHistory.ENTRIES.put(line, entry);
                allMessages.add(line);
                changed = true;
            }
        }
        if (changed) textCenterer$refreshHistory();
    }

    @Override
    public void textCenterer$refreshHistory() {
        if (allMessages.size() > TextCentererConfig.historyLimit())
            allMessages.subList(TextCentererConfig.historyLimit(), allMessages.size()).clear();
        chatScrollbarPos = 0;
        refreshTrimmedMessages();
        chatScrollbarPos = 0;
        ChatSelectionRenderer.SELECTION.clear();
    }
}
