package kim.biryeong.textcenterer.chat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Bounded, plain-text archive shared by all worlds and servers. */
public final class ChatArchive {
    public record Entry(String id, long time, String text) {}
    private final Path path;
    private final ArrayList<Entry> entries = new ArrayList<>();
    private boolean persistent, loaded, dirty;
    private int limit;
    public ChatArchive(Path path, int limit) { this.path = path; this.limit = limit; }
    public synchronized void configure(boolean enabled, int limit) throws IOException {
        this.limit = limit;
        if (enabled && !loaded) {
            List<Entry> disk = read(path);
            Set<String> current = new HashSet<>();
            entries.forEach(e -> current.add(e.id));
            for (Entry entry : disk) if (current.add(entry.id)) entries.add(entry);
            entries.sort(Comparator.comparingLong(Entry::time));
            loaded = true;
        }
        persistent = enabled;
        trim();
        if (enabled) { dirty = true; save(); }
    }
    public synchronized Entry add(long time, String text) {
        Entry entry = new Entry(UUID.randomUUID().toString(), time, text);
        entries.add(entry);
        trim();
        dirty = true;
        return entry;
    }
    private void trim() { if (entries.size() > limit) entries.subList(0, entries.size() - limit).clear(); }
    public synchronized List<Entry> entries() { return List.copyOf(entries); }
    public synchronized List<Entry> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.text.toLowerCase(Locale.ROOT).contains(needle)) result.add(entry);
        }
        return result;
    }
    public synchronized void clear() throws IOException { entries.clear(); dirty = true; save(); }
    public synchronized void remove(String id) { if (entries.removeIf(e -> e.id.equals(id))) dirty = true; }
    public synchronized void save() throws IOException {
        if (!persistent || !dirty) return;
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
            out.writeInt(0x54434831);
            out.writeInt(entries.size());
            for (Entry entry : entries) {
                out.writeUTF(entry.id);
                out.writeLong(entry.time);
                byte[] text = entry.text.getBytes(StandardCharsets.UTF_8);
                out.writeInt(text.length);
                out.write(text);
            }
        }
        try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        dirty = false;
    }
    private static List<Entry> read(Path path) throws IOException {
        if (!Files.exists(path)) return List.of();
        List<Entry> result = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (in.readInt() != 0x54434831) throw new IOException("Unknown chat archive format");
            int count = in.readInt();
            if (count < 0 || count > 50000) throw new IOException("Invalid chat archive count");
            for (int i = 0; i < count; i++) {
                String id = in.readUTF();
                long time = in.readLong();
                int length = in.readInt();
                if (length < 0 || length > 1048576) throw new IOException("Invalid chat message length");
                byte[] bytes = in.readNBytes(length);
                if (bytes.length != length) throw new EOFException("Truncated chat archive");
                result.add(new Entry(id, time, new String(bytes, StandardCharsets.UTF_8)));
            }
        }
        return result;
    }
}
