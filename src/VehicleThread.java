import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the lifecycle of an individual vehicle making a round trip.
 * Manages toll entry, queue joining, waiting for ferry transport, and the return trip.
 */
public class VehicleThread extends Thread {
    private final Vehicle vehicle;
    private final TollBooth[] tollsSideA;
    private final TollBooth[] tollsSideB;
    private final WaitingArea waitingAreaA;
    private final WaitingArea waitingAreaB;
    private final Ferry ferry;
    private final Statistics stats;

    public VehicleThread(Vehicle vehicle, TollBooth[] tollsA, TollBooth[] tollsB,
                         WaitingArea waitA, WaitingArea waitB, Ferry ferry, Statistics stats) {
        this.vehicle = vehicle;
        this.tollsSideA = tollsA;
        this.tollsSideB = tollsB;
        this.waitingAreaA = waitA;
        this.waitingAreaB = waitB;
        this.ferry = ferry;
        this.stats = stats;
    }

    @Override
    public void run() {
        vehicle.setStartTime(System.currentTimeMillis());
        try {
            // First leg: Travel from origin side to the destination side
            performTrip(vehicle.getCurrentSide(), vehicle.getOriginSide().getOpposite());

            // Random waiting time at destination before the return trip (0.5–2 sec)
            int waitBeforeReturn = ThreadLocalRandom.current().nextInt(500, 2000);
            Logger.log(String.format("%s waiting %.1f sec before return trip",
                    vehicle, waitBeforeReturn / 1000.0));
            Thread.sleep(waitBeforeReturn);

            // Second leg: Return trip back to origin
            performTrip(vehicle.getCurrentSide(), vehicle.getOriginSide());

            // Log completion and stats
            vehicle.setEndTime(System.currentTimeMillis());
            vehicle.setCompleted(true);
            stats.addWaitTime(vehicle.getTotalWaitTime());
            stats.recordVehicleCompleted();
            Logger.log(String.format("%s COMPLETED full round trip", vehicle));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log(String.format("%s interrupted", vehicle));
        }
    }

    /**
     * Handles the logic for passing the toll, joining the queue, and waiting for the ferry.
     */
    private void performTrip(Side startSide, Side destination) throws InterruptedException {
        // 1. Pass through toll booth
        TollBooth[] tolls = (startSide == Side.A) ? tollsSideA : tollsSideB;
        int randomBooth = ThreadLocalRandom.current().nextInt(tolls.length);

        long beforeToll = System.currentTimeMillis();
        tolls[randomBooth].pass(vehicle);
        vehicle.addTollTime(System.currentTimeMillis() - beforeToll);

        // 2. Enter waiting area queue
        WaitingArea waitingArea = (startSide == Side.A) ? waitingAreaA : waitingAreaB;
        waitingArea.enter(vehicle);

        // Notify ferry a vehicle has arrived in case it's waiting
        ferry.signalVehicleArrived();

        // 3. Block and wait until the vehicle is transported to destination side
        synchronized (vehicle) {
            while (vehicle.getCurrentSide() != destination) {
                vehicle.wait();
            }
        }

        // 4. Record travel and boarding timestamps upon arrival
        long unloadTime = System.currentTimeMillis();
        long boardingTime = vehicle.getBoardingStartTime();
        if (boardingTime > 0) {
            vehicle.addTravelTime(unloadTime - boardingTime);
        }

        if (vehicle.getFirstDepartureTime() == 0) {
            vehicle.setFirstDepartureTime(boardingTime);
        }
    }
}