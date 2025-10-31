import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class LatencyTracker {
    private static final AtomicLong totalLatency = new AtomicLong();
    private static final AtomicInteger count = new AtomicInteger();

    public static void recordLatency(long latency) {
        totalLatency.addAndGet(latency);
        count.incrementAndGet();
    }

    public static double getAverageLatency() {
        int c = count.get();
        return c == 0 ? 0 : (double) totalLatency.get() / c;
    }

    public static void reset() {
        totalLatency.set(0);
        count.set(0);
    }
}
