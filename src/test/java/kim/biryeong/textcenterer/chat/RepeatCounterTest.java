package kim.biryeong.textcenterer.chat;

public final class RepeatCounterTest {
    private static void expect(long actual, long expected) {
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        RepeatCounter<String> counter = new RepeatCounter<>();
        expect(counter.accept("안녕하세요", false), 1);
        for (int i = 2; i <= 7; i++) expect(counter.accept("안녕하세요", true), i);
        expect(counter.accept("다른 메시지", true), 1);
        expect(counter.accept("안녕하세요", true), 1);
        expect(counter.accept("안녕하세요", true), 2);
        expect(counter.accept("안녕하세요", false), 1);
        counter.reset();
        expect(counter.accept("안녕하세요", true), 1);
        expect(counter.accept("안녕하세요 ×7", true), 1);
        expect(counter.accept("안녕하세요 ×7", true), 2);
        expect(counter.accept("안녕하세요 ×7 ", true), 1);
        System.out.println("PASS: repeat counts, interrupted runs, clear/rebuild, removed lines and literal suffixes");
    }
}
