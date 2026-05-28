public class FerryThread extends Thread {
    private final Ferry ferry;

    public FerryThread(Ferry ferry) {
        this.ferry = ferry;
    }

    @Override
    public void run() {
        try {
            while (ferry.isRunning()) {
                // Block until it's time to depart; load happens atomically inside.
                ferry.waitForAndLoadOrDepart();
                ferry.departAndTravel();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("Ferry thread interrupted");
        }
    }
}