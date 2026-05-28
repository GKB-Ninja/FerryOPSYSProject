import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

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
