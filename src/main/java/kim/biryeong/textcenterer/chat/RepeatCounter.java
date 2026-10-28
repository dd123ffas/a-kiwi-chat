package kim.biryeong.textcenterer.chat;

import java.util.Objects;

/** Counts consecutive equal messages, including their formatting/indicator key. */
public final class RepeatCounter<T> {
    private T previous;
    private long count;

    public long accept(T message, boolean previousStillVisible) {
        count = previousStillVisible && count > 0 && Objects.equals(previous, message)
            ? (count == Long.MAX_VALUE ? count : count + 1) : 1;
        previous = message;
        return count;
    }

    public void reset() { previous = null; count = 0; }
}
