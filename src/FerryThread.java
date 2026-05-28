public class FerryThread extends Thread {
    private final Ferry ferry;

    public FerryThread(Ferry ferry) {
        this.ferry = ferry;
    }

    @Override
    public void run() {
        try {
            while (ferry.isRunning()) {
                // Wait until departure conditions are met
                ferry.boardAndDecideDeparture();

                // Stop immediately if shutdown happened during waiting
                if (!ferry.isRunning()) {
                    break;
                }

                ferry.departAndTravel();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            // Only log unexpected interruptions
            if (ferry.isRunning()) {
                Logger.log("Ferry thread interrupted unexpectedly while running.");
            }
            // Else return complete message.
            else {
                Logger.log("All vehicles completed their trip.");
            }
        }
    }
}