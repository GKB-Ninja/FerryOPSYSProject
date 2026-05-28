import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe centralized logging class.
 * Outputs sequential timeline events directly to console and writes them to a designated log file.
 */
public class Logger {
    private static final AtomicLong globalTime = new AtomicLong(0);
    private static PrintWriter fileWriter = null;

    public static void init(String filename) {
        try {
            fileWriter = new PrintWriter(new FileWriter(filename));
        } catch (Exception e) {
            System.err.println("Could not open log file: " + e.getMessage());
        }
    }

    /**
     * Synchronized global logger guarantees strict chronological order of events across threads.
     */
    public static synchronized void log(String message) {
        long time = globalTime.incrementAndGet();
        String logLine = String.format("[%d] %s", time, message);

        System.out.println(logLine);
        if (fileWriter != null) {
            fileWriter.println(logLine);
            fileWriter.flush();
        }
    }

    public static void close() {
        if (fileWriter != null) fileWriter.close();
    }
}