/**
 * Represents the lifecycle thread of the Ferry.
 * Loops to load vehicles, travel, and unload until stopped.
 */
public class FerryThread extends Thread {
    private final Ferry ferry;

    public FerryThread(Ferry ferry) {
        this.ferry = ferry;
    }

    @Override
    public void run() {
        try {
            while (ferry.isRunning()) {
                ferry.boardAndDecideDeparture();

                // Break loop if simulation is terminated during the wait
                if (!ferry.isRunning()) {
                    break;
                }

                ferry.logAndTravel();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            // Distinguish between an unexpected crash vs. clean shutdown
            if (ferry.isRunning()) {
                Logger.log("Ferry thread interrupted unexpectedly while running.");
            } else {
                Logger.log("All vehicles completed their trip. Ferry shutting down safely.");
            }
        }
    }
}