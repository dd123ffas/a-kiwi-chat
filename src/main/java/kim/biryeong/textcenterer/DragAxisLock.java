package kim.biryeong.textcenterer;

/** Locks movement to the first non-zero direction while Shift is held. */
final class DragAxisLock {
    private int axis;

    void reset() { axis = 0; }

    void update(boolean shift, double dx, double dy) {
        if (!shift) reset();
        else if (axis == 0 && (dx != 0 || dy != 0))
            axis = Math.abs(dx) >= Math.abs(dy) ? 1 : 2;
    }

    boolean horizontal() { return axis != 2; }
    boolean vertical() { return axis != 1; }
}
