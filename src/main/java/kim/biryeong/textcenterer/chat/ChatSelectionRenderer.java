package kim.biryeong.textcenterer.chat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class ChatSelectionRenderer {
    public static final ChatTextSelection SELECTION = new ChatTextSelection();
    private static final List<ChatTextSelection.Line> FRAME = new ArrayList<>();
    private static boolean collecting;
    private ChatSelectionRenderer() {}
    public static void beginFrame(boolean enabled) { FRAME.clear(); collecting = enabled; }
    public static void endFrame() { if (collecting) SELECTION.frame(FRAME); collecting = false; }
    public static void draw(ChatGraphicsAccess graphics, GuiMessage.Line line, int top, Matrix3x2f pose) {
        if (!collecting || line == null) return;
        var font = Minecraft.getInstance().font;
        double scale = pose.m00();
        if (scale <= 0) return;
        StringBuilder text = new StringBuilder();
        List<Double> edges = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        edges.add(0.0);
        line.content().accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            colors.add(style.getColor() == null ? null : style.getColor().getValue());
            FormattedCharSequence glyph = visitor -> visitor.accept(0, style, codePoint);
            double advance = font.getSplitter().stringWidth(glyph);
            edges.add(edges.get(edges.size() - 1) + advance * scale);
            return true;
        });
        double[] positions = edges.stream().mapToDouble(Double::doubleValue).toArray();
        Vector2f origin = pose.transformPosition(0, top, new Vector2f());
        FRAME.add(new ChatTextSelection.Line(line, text.toString(), origin.x, origin.y,
            origin.y + font.lineHeight * scale, positions, colors.toArray(Integer[]::new)));
        int[] range = SELECTION.range(line);
        if (range != null && range[1] < positions.length) {
            graphics.fill((int)Math.floor(positions[range[0]] / scale), top,
                (int)Math.ceil(positions[range[1]] / scale), top + font.lineHeight, 0xA05080D0);
        }
    }
}
