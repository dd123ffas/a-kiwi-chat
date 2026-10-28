package kim.biryeong.textcenterer.chat;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.IdentityHashMap;
import kim.biryeong.textcenterer.TextCentererClient;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class ChatHistory {
    public static final ChatArchive ARCHIVE = new ChatArchive(Path.of("config", "text-centerer-history.bin"), TextCentererConfig.historyLimit());
    public static final Map<GuiMessage, ChatArchive.Entry> ENTRIES = new IdentityHashMap<>();
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static long lastSave;
    private ChatHistory() {}
    public static void configure() {
        try { ARCHIVE.configure(TextCentererConfig.persistHistory(), TextCentererConfig.historyLimit()); }
        catch (java.io.IOException e) { TextCentererClient.LOGGER.error("Cannot configure chat archive", e); }
    }
    public static void save() {
        try { ARCHIVE.save(); }
        catch (java.io.IOException e) { TextCentererClient.LOGGER.error("Cannot save chat archive", e); }
    }
    public static ChatArchive.Entry record(GuiMessage line) {
        ChatArchive.Entry entry = ENTRIES.get(line);
        if (entry == null) {
            entry = ARCHIVE.add(System.currentTimeMillis(), line.content().getString());
            ENTRIES.put(line, entry);
            if (ENTRIES.size() > TextCentererConfig.historyLimit() * 2) {
                java.util.Set<String> retained = new java.util.HashSet<>();
                ARCHIVE.entries().forEach(e -> retained.add(e.id()));
                ENTRIES.values().removeIf(e -> !retained.contains(e.id()));
            }
            if (System.currentTimeMillis() - lastSave > 10000) { save(); lastSave = System.currentTimeMillis(); }
        }
        return entry;
    }
    public static Component timestamp(GuiMessage line, Component content) {
        ChatArchive.Entry entry = ENTRIES.get(line);
        if (!TextCentererConfig.timestamps() || entry == null) return content;
        return Component.empty().append(Component.literal("[" + CLOCK.format(Instant.ofEpochMilli(entry.time())) + "] ").withStyle(ChatFormatting.GRAY)).append(content);
    }
    public static String date(ChatArchive.Entry entry) { return DATE.format(Instant.ofEpochMilli(entry.time())); }
}
