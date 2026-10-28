package kim.biryeong.textcenterer;

import kim.biryeong.textcenterer.chat.ChatPositionBounds;
import kim.biryeong.textcenterer.chat.TextCentererConfig;

public final class ChatPositionTest {
    public static void main(String[] args) {
        DragAxisLock lock = new DragAxisLock();
        lock.update(true, 0, 0);
        require(lock.horizontal() && lock.vertical(), "zero movement must not choose an axis");
        lock.update(true, 4, 1);
        require(lock.horizontal() && !lock.vertical(), "horizontal first");
        lock.update(true, 0, 20);
        require(lock.horizontal() && !lock.vertical(), "retain horizontal lock");
        lock.update(false, 0, 0);
        require(lock.horizontal() && lock.vertical(), "release Shift");
        lock.update(true, -1, -4);
        require(!lock.horizontal() && lock.vertical(), "vertical first");
        lock.update(true, 30, 0);
        require(!lock.horizontal() && lock.vertical(), "retain vertical lock");
        lock.reset();
        lock.update(true, -4, 1);
        require(lock.horizontal() && !lock.vertical(), "new drag resets lock");
        for (int size : new int[]{320, 427, 640, 960, 1920}) {
            for (int span : new int[]{20, 100, 280}) {
                for (double start : new double[]{-1500.75, -10.5, 0, 24.25, 2000.75}) {
                    int correction = ChatPositionBounds.correction(start, start + span, size);
                    require(start + correction >= 0 && start + span + correction <= size, "fit viewport");
                    require(ChatPositionBounds.correction(start + correction, start + span + correction, size) == 0,
                        "fitting must be stable");
                }
            }
        }
        require(ChatPositionBounds.correction(-40, 360, 320) == 0, "oversized extent centered");
        TextCentererConfig.previewChatYOffset(-240);
        require(TextCentererConfig.chatYOffset() == -240, "allow below-screen positioning");
        TextCentererConfig.previewChatYOffset(2000);
        require(TextCentererConfig.chatYOffset() == 2000, "allow above-screen positioning");
        // fullscreen -> windowed fits the chat, back to fullscreen restores the user's position
        TextCentererConfig.previewChatXOffset(600);
        TextCentererConfig.previewChatYOffset(500);
        TextCentererConfig.fitChatOffset(600, 500, 200, 150);
        require(TextCentererConfig.chatXOffset() == 200 && TextCentererConfig.chatYOffset() == 150, "fitted position");
        require(TextCentererConfig.userChatXOffset() == 600 && TextCentererConfig.userChatYOffset() == 500,
            "fit keeps the user's position");
        TextCentererConfig.previewChatXOffset(-30);
        require(TextCentererConfig.userChatXOffset() == -30 && TextCentererConfig.userChatYOffset() == 500,
            "a user move after a fit becomes the user's position");
        System.out.println("Chat position and axis lock checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
