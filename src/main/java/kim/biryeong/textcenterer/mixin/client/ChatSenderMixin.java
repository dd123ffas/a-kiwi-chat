package kim.biryeong.textcenterer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.util.UUID;
import kim.biryeong.textcenterer.chat.BuiltinChatHeads;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatListener.class)
abstract class ChatSenderMixin {
    @WrapMethod(method = "showMessageToPlayer")
    private boolean textCenterer$captureSender(ChatType.Bound params, PlayerChatMessage message,
            Component decorated, GameProfile sender, boolean secureOnly, Instant timestamp, Operation<Boolean> original) {
        UUID previous = BuiltinChatHeads.processingSender;
        BuiltinChatHeads.processingSender = sender.id();
        try { return original.call(params, message, decorated, sender, secureOnly, timestamp); }
        finally { BuiltinChatHeads.processingSender = previous; }
    }
}
