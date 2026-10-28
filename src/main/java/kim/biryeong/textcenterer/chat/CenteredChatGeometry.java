package kim.biryeong.textcenterer.chat;

public record CenteredChatGeometry(int textX, int backgroundMinX, int backgroundMaxX, int topY, int bottomY) {
    public double relativeTextX(double mouseX, double chatScale) {
        return mouseX / chatScale - 4.0 - textX;
    }

    public static CenteredChatGeometry forLine(int n, int n2, double d, int n3, int n4, int n5, int n6) {
        return forLine(n, n2, d, n3, n4, n5, n6, 0);
    }

    public static CenteredChatGeometry forLine(int n, int n2, double d, int n3, int n4, int n5, int n6, int headWidth) {
        if (d <= 0.0) {
            throw new IllegalArgumentException("chatScale must be positive");
        }
        int n7 = (int)Math.floor(((double)n / 2.0 + TextCentererConfig.chatXOffset()) / d) - 4;
        int n8 = n7 - switch (TextCentererConfig.textAlignment()) {
            case LEFT -> -headWidth;
            case CENTER -> n6 / 2;
            case RIGHT -> n6;
        };
        int n9 = n4 - n5 * n3;
        return new CenteredChatGeometry(n8, n8 - 4, n8 + n6 + 4, n9 - n3, n9);
    }
}
