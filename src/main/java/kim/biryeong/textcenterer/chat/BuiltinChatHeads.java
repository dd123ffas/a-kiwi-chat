package kim.biryeong.textcenterer.chat;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ARGB;

public final class BuiltinChatHeads {
    public static int width() { return TextCentererConfig.headSize() + 2; }
    // Scoped to actual processing, so delayed/filtered chat cannot leak a sender.
    public static UUID processingSender;

    private BuiltinChatHeads() {}

    public static void capture(GuiMessage line) {
        var network = Minecraft.getInstance().getConnection();
        UUID inferred = network == null ? null : HeadDetection.infer(line.content().getString(),
            network.getOnlinePlayers().stream().map(player -> new HeadDetection.Candidate(
                player.getProfile().id(), player.getProfile().name(),
                player.getTabListDisplayName() == null ? null : player.getTabListDisplayName().getString())).toList());
        ((ChatHeadAccess)(Object)line).textCenterer$head(new HeadDetection.Result(processingSender, inferred));
    }

    public static UUID sender(GuiMessage.Line line) {
        HeadDetection.Result head = ((ChatHeadAccess)(Object)line).textCenterer$head();
        return head == null ? null : head.select(TextCentererConfig.headDetectionMode());
    }

    public static void extractRenderState(GuiGraphicsExtractor context, GuiMessage.Line line, int textX, int y, float alpha) {
        UUID sender = sender(line);
        if (sender == null) return;
        var network = Minecraft.getInstance().getConnection();
        var player = network == null ? null : network.getPlayerInfo(sender);
        var skin = player == null ? DefaultPlayerSkin.get(sender) : player.getSkin();
        PlayerFaceExtractor.extractRenderState(context, skin, textX - width(), y + TextCentererConfig.headYOffset(), TextCentererConfig.headSize(), ARGB.white(alpha));
    }
}
