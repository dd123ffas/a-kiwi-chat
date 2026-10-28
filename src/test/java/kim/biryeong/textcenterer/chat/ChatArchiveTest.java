package kim.biryeong.textcenterer.chat;

import java.nio.file.*;
import java.io.IOException;

public final class ChatArchiveTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path root = Files.createDirectories(Path.of("build", "archive-check"));
        Path path = Files.createTempDirectory(root, "run-").resolve("history.bin");
        ChatArchive archive = new ChatArchive(path, 3);
        archive.configure(false, 3);
        archive.add(1, "첫 메시지");
        archive.add(2, "Hello 😀\n다음 줄");
        archive.save();
        check(!Files.exists(path), "OFF wrote an archive");
        check(archive.search("HELLO").size() == 1, "Case-insensitive search");
        check(archive.search("😀").size() == 1, "Unicode search");
        check(archive.search(".*").isEmpty(), "Query must be literal");
        archive.configure(true, 3);
        ChatArchive restarted = new ChatArchive(path, 3);
        restarted.configure(true, 3);
        check(restarted.entries().equals(archive.entries()), "Restart changed content or timestamps");
        restarted.add(3, "서버 B");
        restarted.add(4, "월드 C");
        restarted.save();
        check(restarted.entries().size() == 3 && restarted.entries().get(0).time() == 2, "Bounded retention");
        restarted.configure(false, 3);
        restarted.add(5, "저장하지 않음");
        restarted.save();
        ChatArchive disk = new ChatArchive(path, 3);
        disk.configure(true, 3);
        check(disk.search("저장하지").isEmpty(), "OFF did not stop writes");
        String removed = disk.entries().get(0).id();
        disk.remove(removed);
        disk.save();
        ChatArchive deleted = new ChatArchive(path, 3);
        deleted.configure(true, 3);
        check(deleted.entries().stream().noneMatch(e -> e.id().equals(removed)), "Deletion did not persist");
        deleted.clear();
        ChatArchive empty = new ChatArchive(path, 3);
        empty.configure(true, 3);
        check(empty.entries().isEmpty(), "Clear did not persist");
        Files.write(path, new byte[]{1, 2, 3});
        try { new ChatArchive(path, 3).configure(true, 3); throw new AssertionError("Corrupt archive accepted"); }
        catch (IOException expected) { check(Files.size(path) == 3, "Corrupt input overwritten"); }
        ChatArchive large = new ChatArchive(path.resolveSibling("large.bin"), 10000);
        large.configure(false, 10000);
        for (int i = 0; i < 10125; i++) large.add(100, "message " + i);
        check(large.entries().size() == 10000 && large.entries().get(0).text().equals("message 125"), "10000 message limit");
        check(!large.entries().get(0).id().equals(large.entries().get(1).id()), "Same-time messages need distinct identities");
        large.configure(true, 10000);
        ChatArchive largeRestart = new ChatArchive(path.resolveSibling("large.bin"), 10000);
        largeRestart.configure(true, 10000);
        check(largeRestart.entries().equals(large.entries()), "10000-message restart round trip");
        System.out.println("PASS: archive OFF/ON, restart, Unicode, search, bounds, deletion, clear, corruption");
    }
}
