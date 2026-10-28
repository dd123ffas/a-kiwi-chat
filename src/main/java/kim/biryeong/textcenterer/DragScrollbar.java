package kim.biryeong.textcenterer;

/** Shared scrollbar geometry and pointer capture, in either rows or pixels. */
final class DragScrollbar {
    private int left, top, trackHeight, thumbHeight, thumbTop, maximum;
    private boolean dragging;
    private double grab;
    private double preciseThumbTop;

    void layout(int left, int top, int trackHeight, int visible, int total, int offset) {
        this.left = left;
        this.top = top;
        this.trackHeight = Math.max(0, trackHeight);
        maximum = Math.max(0, total - visible);
        thumbHeight = Math.min(this.trackHeight, Math.max(12,
            (int)((long)this.trackHeight * Math.max(0, visible) / Math.max(1, total))));
        preciseThumbTop = top + (maximum == 0 ? 0 :
            (this.trackHeight - thumbHeight) * (double)Math.clamp(offset, 0, maximum) / maximum);
        thumbTop = (int)Math.round(preciseThumbTop);
        if (!visible()) reset();
    }

    boolean visible() { return maximum > 0 && trackHeight > thumbHeight; }
    int left() { return left; }
    int top() { return top; }
    int bottom() { return top + trackHeight; }
    int thumbTop() { return thumbTop; }
    int thumbBottom() { return thumbTop + thumbHeight; }
    boolean dragging() { return dragging; }

    boolean click(double x, double y, int button) {
        if (button != 0 || !visible() || x < left - 4 || x >= left + 6 || y < top || y >= bottom()) return false;
        dragging = true;
        grab = y >= thumbTop && y < thumbBottom() ? y - preciseThumbTop : thumbHeight / 2.0;
        return true;
    }

    int offsetAt(double y) {
        if (!visible()) return 0;
        return (int)Math.round(Math.clamp((y - top - grab) / (trackHeight - thumbHeight), 0, 1) * maximum);
    }

    boolean release(int button) {
        if (button != 0 || !dragging) return false;
        reset();
        return true;
    }

    void reset() { dragging = false; }
}
