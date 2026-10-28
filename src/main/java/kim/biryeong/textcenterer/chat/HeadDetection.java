package kim.biryeong.textcenterer.chat;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** Pure sender selection: heuristics are deliberately separate from packet identity. */
public final class HeadDetection {
    public enum Mode {
        UUID_ONLY, HEURISTIC_ONLY, BOTH;
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
        public static Mode parse(String value) {
            try { return valueOf(value); }
            catch (IllegalArgumentException | NullPointerException e) { return BOTH; }
        }
    }
    public record Candidate(UUID id, String name, String displayName) {}
    public record Result(UUID packet, UUID heuristic) {
        public UUID select(Mode mode) {
            return switch (mode) {
                case UUID_ONLY -> packet;
                case HEURISTIC_ONLY -> heuristic;
                case BOTH -> packet != null ? packet : heuristic;
            };
        }
    }
    private HeadDetection() {}

    public static UUID infer(String message, List<Candidate> players) {
        String text = clean(message).stripLeading();
        // Only inspect a sender prefix, never a name mentioned in the message body.
        UUID found = null;
        for (Candidate player : players) {
            if (matches(text, player.name()) || matches(text, player.displayName())) {
                if (found != null && !found.equals(player.id())) return null;
                found = player.id();
            }
        }
        return found;
    }

    private static boolean matches(String text, String name) {
        if (name == null || name.isBlank()) return false;
        String quoted = Pattern.quote(clean(name).strip());
        // Vanilla <name>, /me '* name', and common [rank] name: / name > / name » formats.
        return Pattern.compile("^(?:\\[[^\\]\\r\\n]{1,48}\\]\\s*){0,3}(?:<" + quoted
            + ">\\s|" + quoted + "\\s*[:：>»›]\\s*|\\*\\s+" + quoted + "\\s+)")
            .matcher(text).find();
    }

    private static String clean(String text) { return text.replaceAll("§[0-9A-FK-ORa-fk-or]", ""); }
}
