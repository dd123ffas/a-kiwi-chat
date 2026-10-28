package kim.biryeong.textcenterer.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Properties;

public final class TextCentererConfig {
    public enum TextAlignment {
        LEFT, CENTER, RIGHT;
        public TextAlignment next() { return values()[(ordinal() + 1) % values().length]; }
        public static TextAlignment parse(String value) {
            try { return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException | NullPointerException exception) { return CENTER; }
        }
    }
    private static TextAlignment textAlignment = TextAlignment.parse(loadProperties().getProperty("text_alignment", "CENTER"));
    public static TextAlignment textAlignment() { return textAlignment; }
    public static void previewTextAlignment(TextAlignment value) { textAlignment = java.util.Objects.requireNonNull(value); }
    public static void cycleTextAlignment() { textAlignment = textAlignment.next(); }

    private static int headSize = loadHeadSize();
    public static int headSize() { return headSize; }
    public static void previewHeadSize(int value) { headSize = Math.max(1, Math.min(32, value)); }
    private static int loadHeadSize() {
        try { return Math.max(1, Math.min(32, Integer.parseInt(loadProperties().getProperty("head_size", "8").trim()))); }
        catch (NumberFormatException e) { return 8; }
    }
    private static int headYOffset = loadHeadYOffset();
    public static int headYOffset() { return headYOffset; }
    public static void previewHeadYOffset(int value) { headYOffset = Math.max(-32, Math.min(32, value)); }
    private static int loadHeadYOffset() {
        try { return Math.max(-32, Math.min(32, Integer.parseInt(loadProperties().getProperty("head_y_offset", "0").trim()))); }
        catch (NumberFormatException e) { return 0; }
    }
    // 0 follows the video GUI scale; 1+ renders the chat at that GUI scale regardless.
    private static int chatGuiScale = loadChatGuiScale();
    public static int chatGuiScale() { return chatGuiScale; }
    public static void previewChatGuiScale(int value) { chatGuiScale = Math.max(0, Math.min(16, value)); }
    private static int loadChatGuiScale() {
        try { return Math.max(0, Math.min(16, Integer.parseInt(loadProperties().getProperty("chat_gui_scale", "0").trim()))); }
        catch (NumberFormatException e) { return 0; }
    }
    private static HeadDetection.Mode headDetectionMode = HeadDetection.Mode.parse(loadProperties().getProperty("head_detection", "BOTH"));
    public static HeadDetection.Mode headDetectionMode() { return headDetectionMode; }
    public static void cycleHeadDetectionMode() { headDetectionMode = headDetectionMode.next(); }
    private static int chatTextYOffset = loadChatTextYOffset();
    public static int chatTextYOffset() { return chatTextYOffset; }
    public static void previewChatTextYOffset(int value) { chatTextYOffset = Math.max(-32, Math.min(32, value)); }
    private static int loadChatTextYOffset() {
        try { return Math.max(-32, Math.min(32, Integer.parseInt(loadProperties().getProperty("chat_text_y_offset", "0").trim()))); }
        catch (NumberFormatException exception) { return 0; }
    }
    public static final int DEFAULT_CHAT_Y_OFFSET = 77;
    private static volatile int chatYOffset = TextCentererConfig.loadChatYOffset();
    private static volatile int chatXOffset = loadChatXOffset();
    private static volatile boolean responsiveBackground = Boolean.parseBoolean(loadProperties().getProperty("responsive_background", "true"));
    private static boolean timestamps = Boolean.parseBoolean(loadProperties().getProperty("timestamps", "false"));
    private static boolean persistHistory = Boolean.parseBoolean(loadProperties().getProperty("persist_history", "false"));
    private static int historyLimit = loadHistoryLimit();
    private static boolean messageStacking = Boolean.parseBoolean(loadProperties().getProperty("message_stacking", "true"));
    public static boolean messageStacking() { return messageStacking; }
    public static void toggleMessageStacking() { messageStacking = !messageStacking; }
    public static boolean timestamps() { return timestamps; }
    public static void toggleTimestamps() { timestamps = !timestamps; }
    public static boolean persistHistory() { return persistHistory; }
    public static void togglePersistHistory() { persistHistory = !persistHistory; }
    public static int historyLimit() { return historyLimit; }
    public static void previewHistoryLimit(int value) { historyLimit = Math.max(1000, Math.min(50000, value)); }
    public static void cycleHistoryLimit() { historyLimit = historyLimit == 1000 ? 10000 : historyLimit == 10000 ? 50000 : 1000; }
    private static int loadHistoryLimit() {
        try { return Math.max(1000, Math.min(50000, Integer.parseInt(loadProperties().getProperty("history_limit", "10000")))); }
        catch (NumberFormatException e) { return 10000; }
    }

    public static int chatXOffset() { return chatXOffset; }
    // Position fitted to the window after a resize; the user's position underneath is the one saved.
    private static int userChatXOffset, userChatYOffset;
    private static int fittedChatXOffset = Integer.MIN_VALUE, fittedChatYOffset = Integer.MIN_VALUE;
    public static int userChatXOffset() { return chatXOffset == fittedChatXOffset ? userChatXOffset : chatXOffset; }
    public static int userChatYOffset() { return chatYOffset == fittedChatYOffset ? userChatYOffset : chatYOffset; }
    public static void fitChatOffset(int userX, int userY, int x, int y) {
        userChatXOffset = userX;
        userChatYOffset = userY;
        chatXOffset = fittedChatXOffset = x;
        chatYOffset = fittedChatYOffset = y;
    }
    public static void previewChatXOffset(int value) { chatXOffset = value; }
    public static boolean responsiveBackground() { return responsiveBackground; }
    public static void previewResponsiveBackground(boolean value) { responsiveBackground = value; }

    private static int loadChatXOffset() {
        try { return Integer.parseInt(loadProperties().getProperty("chat_x_offset", "0").trim()); }
        catch (NumberFormatException exception) { return 0; }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        Path path = Path.of("config", "text-centerer.properties");
        if (Files.exists(path)) {
            try (InputStream stream = Files.newInputStream(path)) { properties.load(stream); }
            catch (IOException exception) { /* Use defaults. */ }
        }
        return properties;
    }

    private TextCentererConfig() {
    }

    public static int chatYOffset() {
        return chatYOffset;
    }

    public static int chatYOffsetDelta() {
        return chatYOffset - 40;
    }

    public static void previewChatYOffset(int n) {
        chatYOffset = n;
    }

    public static void setChatYOffset(int n) {
        previewChatYOffset(n);
        Properties properties = new Properties();
        properties.setProperty("chat_text_y_offset", String.valueOf(chatTextYOffset));
        properties.setProperty("chat_y_offset", String.valueOf(userChatYOffset()));
        properties.setProperty("text_alignment", textAlignment.name());
        properties.setProperty("chat_x_offset", String.valueOf(userChatXOffset()));
        properties.setProperty("responsive_background", String.valueOf(responsiveBackground));
        properties.setProperty("timestamps", String.valueOf(timestamps));
        properties.setProperty("persist_history", String.valueOf(persistHistory));
        properties.setProperty("history_limit", String.valueOf(historyLimit));
        properties.setProperty("message_stacking", String.valueOf(messageStacking));
        properties.setProperty("head_detection", headDetectionMode.name());
        properties.setProperty("head_y_offset", String.valueOf(headYOffset));
        properties.setProperty("head_size", String.valueOf(headSize));
        properties.setProperty("chat_gui_scale", String.valueOf(chatGuiScale));
        try {
            Path path = Path.of("config", "text-centerer.properties");
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            try (OutputStream outputStream = Files.newOutputStream(path, new OpenOption[0]);){
                properties.store(outputStream, "A kiwi chat settings");
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static int loadChatYOffset() {
        Path path = Path.of("config", "text-centerer.properties");
        Properties properties = new Properties();
        try {
            if (Files.exists(path, new LinkOption[0])) {
                try (InputStream inputStream = Files.newInputStream(path, new OpenOption[0]);){
                    properties.load(inputStream);
                }
            }
            return Integer.parseInt(properties.getProperty("chat_y_offset", String.valueOf(DEFAULT_CHAT_Y_OFFSET)).trim());
        }
        catch (IOException | NumberFormatException exception) {
            return DEFAULT_CHAT_Y_OFFSET;
        }
    }
}
