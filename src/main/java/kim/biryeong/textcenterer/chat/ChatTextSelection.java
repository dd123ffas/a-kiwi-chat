package kim.biryeong.textcenterer.chat;

import java.util.Comparator;
import java.util.List;

/** Selection uses rendered code-point boundaries, never UTF-16 halves. */
public final class ChatTextSelection {
    public record Line(Object id, String text, double left, double top, double bottom,
                       double[] edges, Integer[] colors) {
        public Line(Object id, String text, double left, double top, double bottom, double[] edges) {
            this(id, text, left, top, bottom, edges, new Integer[edges.length - 1]);
        }
        public int length() { return edges.length - 1; }
        public int caret(double x) {
            for (int i = 0; i < length(); i++) {
                if (x < left + (edges[i] + edges[i + 1]) / 2) return i;
            }
            return length();
        }
        public String slice(int from, int to) {
            return text.substring(text.offsetByCodePoints(0, from), text.offsetByCodePoints(0, to));
        }
    }
    private record Point(int line, int character) implements Comparable<Point> {
        public int compareTo(Point other) {
            return line == other.line ? Integer.compare(character, other.character) : Integer.compare(line, other.line);
        }
    }
    private List<Line> frame = List.of();
    private List<Line> selectedLines = List.of();
    private Point anchor, end;
    private boolean pressed, dragged;
    private double pressX, pressY;
    private double lastX, lastY;

    public void frame(List<Line> lines) {
        frame = lines.stream().sorted(Comparator.comparingDouble(Line::top)).toList();
        if (anchor == null) return;
        int first = Math.min(anchor.line, end.line), last = Math.max(anchor.line, end.line);
        int start = indexOf(frame, selectedLines.get(first).id);
        // Rebase by identity, never by row number or equal text. Keep the copy
        // snapshot if any selected line has moved out of view or been replaced.
        for (int i = first; i <= last; i++) {
            int next = start + i - first;
            if (start < 0 || next >= frame.size()
                    || frame.get(next).id != selectedLines.get(i).id) return;
        }
        anchor = new Point(start + anchor.line - first, anchor.character);
        end = new Point(start + end.line - first, end.character);
        selectedLines = frame;
    }
    private static int indexOf(List<Line> lines, Object id) {
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).id == id) return i;
        return -1;
    }
    public boolean begin(double x, double y) {
        clear();
        for (int i = 0; i < frame.size(); i++) {
            Line line = frame.get(i);
            if (y >= line.top && y < line.bottom && x >= line.left && x <= line.left + line.edges[line.length()]) {
                selectedLines = frame;
                anchor = end = new Point(i, line.caret(x));
                pressed = true;
                pressX = x;
                pressY = y;
                lastX = x;
                lastY = y;
                return true;
            }
        }
        return false;
    }
    public boolean drag(double x, double y) {
        if (!pressed) return false;
        if (x == lastX && y == lastY) return true;
        lastX = x;
        lastY = y;
        if (Math.hypot(x - pressX, y - pressY) >= 2) dragged = true;
        if (dragged && !frame.isEmpty()) {
            int index = frame.size() - 1;
            for (int i = 0; i < frame.size(); i++) {
                if (y < frame.get(i).bottom) { index = i; break; }
            }
            Line target = frame.get(index);
            int selectedIndex = indexOf(selectedLines, target.id);
            if (selectedIndex >= 0) end = new Point(selectedIndex, target.caret(x));
        }
        return true;
    }
    public boolean pressed() { return pressed; }
    public boolean release() {
        if (pressed && !dragged && anchor != null) {
            int line = anchor.line;
            anchor = new Point(line, 0);
            end = new Point(line, selectedLines.get(line).length());
        }
        pressed = false;
        return dragged;
    }
    public void clear() { anchor = end = null; selectedLines = List.of(); pressed = dragged = false; }
    public boolean hasSelection() { return anchor != null && anchor.compareTo(end) != 0; }
    public int[] range(Object id) {
        if (!hasSelection()) return null;
        Point first = anchor.compareTo(end) < 0 ? anchor : end;
        Point last = anchor.compareTo(end) < 0 ? end : anchor;
        for (int i = first.line; i <= last.line; i++) {
            Line line = selectedLines.get(i);
            if (line.id == id) return new int[]{i == first.line ? first.character : 0,
                i == last.line ? last.character : line.length()};
        }
        return null;
    }
    private static String colorCode(int rgb) {
        int[] palette = {0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
            0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};
        for (int i = 0; i < palette.length; i++) if (palette[i] == rgb) return "§" + Integer.toHexString(i);
        StringBuilder code = new StringBuilder("§x");
        for (char digit : String.format(java.util.Locale.ROOT, "%06x", rgb & 0xFFFFFF).toCharArray())
            code.append('§').append(digit);
        return code.toString();
    }
    public String text() { return selectedText(false); }
    // Copy plain text only, including legacy § codes some servers embed in literal text.
    public String clipboardText() { return stripFormatting(selectedText(false)); }
    public static String stripFormatting(String text) { return text.replaceAll("§[0-9A-FK-ORXa-fk-orx]", ""); }
    private String selectedText(boolean includeColors) {
        if (!hasSelection()) return "";
        StringBuilder result = new StringBuilder();
        Point first = anchor.compareTo(end) < 0 ? anchor : end;
        Point last = anchor.compareTo(end) < 0 ? end : anchor;
        for (int i = first.line; i <= last.line; i++) {
            Line line = selectedLines.get(i);
            int from = i == first.line ? first.character : 0;
            int to = i == last.line ? last.character : line.length();
            Integer previous = null;
            for (int c = from; c < to; c++) {
                Integer color = line.colors[c];
                if (includeColors && !java.util.Objects.equals(previous, color)) {
                    if (color == null) result.append("§r");
                    else result.append(colorCode(color));
                    previous = color;
                }
                result.append(line.slice(c, c + 1));
            }
            if (includeColors && previous != null) result.append("§r");
            if (i < last.line) result.append('\n');
        }
        return result.toString();
    }
}
