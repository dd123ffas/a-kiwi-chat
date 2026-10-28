package kim.biryeong.textcenterer.chat;

/** Translation needed to fit an extent; center it if larger than the viewport. */
public final class ChatPositionBounds {
    private ChatPositionBounds() {}

    public static int correction(double start, double end, int viewport) {
        if (end - start > viewport) return (int)Math.round((viewport - start - end) / 2);
        if (start < 0) return (int)Math.ceil(-start);
        if (end > viewport) return (int)Math.floor(viewport - end);
        return 0;
    }
}
