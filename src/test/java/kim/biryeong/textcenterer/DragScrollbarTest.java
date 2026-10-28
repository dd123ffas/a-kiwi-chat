package kim.biryeong.textcenterer;

public final class DragScrollbarTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        DragScrollbar bar = new DragScrollbar();
        bar.layout(100, 40, 100, 20, 100, 0);
        check(bar.visible(), "overflow is visible");
        check(!bar.click(100, 45, 1), "ignore right click");
        check(!bar.click(90, 45, 0), "ignore outside track");
        check(bar.click(99, 45, 0), "expanded thumb hit target");
        check(bar.offsetAt(45) == 0, "thumb grab does not jump");
        check(bar.offsetAt(125) == 80, "drag reaches final item");
        check(bar.offsetAt(-200) == 0 && bar.offsetAt(999) == 80, "drag clamps outside viewport");
        check(!bar.release(1) && bar.dragging(), "other button release preserves capture");
        check(bar.release(0) && !bar.dragging(), "left release ends capture");
        check(bar.click(100, 90, 0) && bar.offsetAt(90) == 40, "track click centers thumb");
        bar.reset();

        // Pixel scrolling in long chat archives must not jump when grabbing a
        // thumb whose rendered top was rounded to an integer pixel.
        for (int offset : new int[] {0, 1, 12345, 499800}) {
            bar.layout(100, 68, 200, 200, 500000, offset);
            double y = bar.thumbTop() + 4;
            check(bar.click(100, y, 0), "long archive grab");
            check(bar.offsetAt(y) == offset, "long archive no jump: " + offset);
            bar.release(0);
        }
        bar.layout(100, 62, 68, 2, 9, 3);
        check(bar.click(100, bar.thumbTop() + 3, 0), "row-based settings grab");
        check(bar.offsetAt(1000) == 7, "settings final row reachable");
        bar.layout(100, 62, 68, 9, 9, 0);
        check(!bar.visible() && !bar.dragging(), "no overflow clears capture");
        bar.layout(100, 62, 0, 0, 100, 0);
        check(!bar.visible() && bar.offsetAt(100) == 0, "zero viewport is safe");
        System.out.println("PASS: scrollbar track click, thumb grab, drag bounds, release, row/pixel scrolling and resize");
    }
}
