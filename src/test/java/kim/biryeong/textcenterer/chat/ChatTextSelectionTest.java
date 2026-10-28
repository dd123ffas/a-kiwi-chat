package kim.biryeong.textcenterer.chat;

import java.util.List;

public final class ChatTextSelectionTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        int cases = 0;
        for (double scale : new double[]{0.25, 0.5, 0.9, 1}) {
            for (int offset : new int[]{-100, 0, 170}) {
                double left = 300 + offset;
                Object id = new Object();
                var a = new ChatTextSelection.Line(id, "가😀ab", left, 50, 60,
                    new double[]{0, 10 * scale, 20 * scale, 26 * scale, 32 * scale});
                var b = new ChatTextSelection.Line(new Object(), "다음", left + 15, 60, 70,
                    new double[]{0, 10 * scale, 20 * scale});
                ChatTextSelection selection = new ChatTextSelection();
                selection.frame(List.of(b, a));
                check(!selection.begin(left - 1, 55), "Outside click selected text");
                check(selection.begin(left, 55), "Cannot begin");
                selection.drag(left + 20 * scale, 55);
                check(selection.release(), "Drag interpreted as link click");
                check(selection.text().equals("가😀"), "Unicode partial selection");
                check(selection.range(id)[1] == 2, "Highlight boundary");
                selection.frame(List.of(a, b));
                check(selection.hasSelection(), "Stable frame clears selection");
                selection.begin(left + 15 + 20 * scale, 65);
                selection.drag(left + 10 * scale, 55);
                selection.release();
                check(selection.text().equals("😀ab\n다음"), "Reverse multi-line selection");
                selection.begin(left + 10 * scale, 55);
                selection.drag(left + 15 + 20 * scale, 65);
                selection.release();
                check(selection.text().equals("😀ab\n다음"), "Forward multi-line selection");
                selection.frame(List.of(b));
                check(selection.hasSelection(), "Leaving view lost copy snapshot");
                selection.frame(List.of(a, b));
                selection.begin(left, 55);
                check(!selection.release() && selection.text().equals(a.text()), "Click must select the entire displayed line");
                check(selection.range(id)[0] == 0 && selection.range(id)[1] == a.length(), "Full-line highlight boundaries");
                check(selection.range(b.id()) == null, "Click selected an adjacent line");
                selection.begin(left, 55);
                selection.drag(left + 1, 55);
                check(!selection.release(), "One-pixel jitter became a drag");
                check(selection.text().equals(a.text()), "Click jitter lost full-line selection");
                selection.begin(left + 20 * scale, 55);
                selection.release();
                check(selection.text().equals(a.text()), "Middle click did not select full line");
                selection.begin(left + 32 * scale, 55);
                selection.release();
                check(selection.text().equals(a.text()), "Right-edge click did not select full line");
                selection.begin(left, 55);
                selection.drag(left + 5, 55);
                selection.drag(left, 55);
                check(selection.release(), "Drag returning to its origin must still suppress link activation");
                check(!selection.hasSelection(), "Returning drag incorrectly became a full-line click");
                selection.begin(left, 55);
                selection.drag(left + 500, 500);
                selection.release();
                check(selection.text().equals("가😀ab\n다음"), "Outside drag should clamp to last line");
                selection.clear();
                check(!selection.pressed() && selection.text().isEmpty(), "Clear failed");
                cases++;
            }
        }
        colorCopy();
        incomingMessages();
        clickedIncomingMessage();
        System.out.println("PASS: " + cases + " selection layouts; Unicode, both directions, multi-line, click/drag and incoming messages");
    }
    private static void colorCopy() {
        Object id = new Object();
        var line = new ChatTextSelection.Line(id, "가😀ab", 0, 0, 10,
            new double[]{0, 10, 20, 30, 40}, new Integer[]{0x55FF55,0x55FF55,0x123456,null});
        ChatTextSelection s = new ChatTextSelection();
        s.frame(List.of(line));
        s.begin(0, 5); s.release();
        check(s.text().equals("가😀ab"), "Plain text changed");
        check(s.clipboardText().equals("가😀ab"), "Full line copied color codes");
        s.begin(10, 5); s.drag(30, 5); s.release();
        check(s.clipboardText().equals("😀a"), "Partial Unicode copied color codes");
        s.frame(List.of());
        check(s.clipboardText().equals("😀a"), "Offscreen selection lost");
        var next = new ChatTextSelection.Line(new Object(), "z", 0, 10, 20,
            new double[]{0,10}, new Integer[]{0xFF5555});
        s.frame(List.of(line, next));
        s.begin(10,15); s.drag(10,5); s.release();
        check(s.clipboardText().equals("😀ab\nz"), "Reverse multiline copied color codes");
        var plain = new ChatTextSelection.Line(new Object(), "hi", 0, 0, 10, new double[]{0,10,20});
        s.frame(List.of(plain)); s.begin(0,5); s.release();
        check(s.clipboardText().equals("hi"), "Uncolored chat gained codes");
        var legacy = new ChatTextSelection.Line(new Object(), "§ax§x§1§2§3§4§5§6y", 0, 0, 10,
            java.util.stream.IntStream.rangeClosed(0, 18).mapToDouble(i -> i * 10).toArray());
        s.frame(List.of(legacy)); s.begin(0,5); s.release();
        check(s.clipboardText().equals("xy"), "Legacy section codes copied");
    }
    private static ChatTextSelection.Line line(Object id, String text, double top) {
        return new ChatTextSelection.Line(id, text, 0, top, top + 10, new double[]{0, 10, 20, 30});
    }
    private static void clickedIncomingMessage() {
        Object a = new Object(), b = new Object();
        ChatTextSelection selection = new ChatTextSelection();
        selection.frame(List.of(line(a, "abc", 20)));
        selection.begin(15, 25);
        selection.frame(List.of(line(a, "abc", 10), line(b, "def", 20)));
        selection.drag(15, 25);
        selection.release();
        check(selection.text().equals("abc"), "Arrival retargeted clicked line");
        check(selection.range(b) == null, "New row inherited click selection");
        selection.frame(List.of(line(b, "def", 20)));
        check(selection.text().equals("abc"), "Clicked copy snapshot lost offscreen");
        check(!selection.begin(100, 25), "Outside text click accepted");
        selection.release();
        check(!selection.hasSelection(), "Outside click retained selection");
    }
    private static void incomingMessages() {
        Object a = new Object(), b = new Object(), c = new Object();
        ChatTextSelection selection = new ChatTextSelection();
        selection.frame(List.of(line(a, "abc", 20), line(b, "def", 30)));
        selection.begin(0, 25);
        selection.drag(20, 25);
        selection.frame(List.of(line(a, "abc", 10), line(b, "def", 20), line(c, "ghi", 30)));
        check(selection.pressed() && selection.text().equals("ab"), "Arrival cleared active drag");
        check(selection.range(a)[1] == 2 && selection.range(b) == null, "Highlight retargeted");
        selection.drag(20, 25);
        check(selection.text().equals("ab"), "Stationary release retargeted selection");
        selection.drag(30, 25);
        check(selection.text().equals("abc\ndef"), "Drag did not use moved row geometry");
        selection.release();
        selection.frame(List.of(line(a, "abc", 0), line(b, "def", 10), line(c, "ghi", 20)));
        check(selection.text().equals("abc\ndef"), "Arrival cleared released selection");
        selection.frame(List.of(line(c, "ghi", 20)));
        check(selection.text().equals("abc\ndef"), "Offscreen selection copy lost");
        selection.frame(List.of(line(new Object(), "abc", 20)));
        check(selection.range(c) == null, "Replacement inherited selection");
    }
}
