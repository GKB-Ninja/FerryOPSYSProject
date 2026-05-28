import java.util.concurrent.ThreadLocalRandom;

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
            // First leg: origin -> destination
            performTrip(vehicle.getCurrentSide(), vehicle.getOriginSide().getOpposite());

            // Random waiting time at destination (0.5–2 sec)
            int waitBeforeReturn = ThreadLocalRandom.current().nextInt(500, 2000);
            Logger.log(String.format("%s waiting %.1f sec before return trip",
                    vehicle, waitBeforeReturn / 1000.0));
            Thread.sleep(waitBeforeReturn);

            // Return trip: back to origin
            performTrip(vehicle.getCurrentSide(), vehicle.getOriginSide());

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

    private void performTrip(Side startSide, Side destination) throws InterruptedException {
        // 1. Toll booth
        TollBooth[] tolls = (startSide == Side.A) ? tollsSideA : tollsSideB;
        int randomBooth = ThreadLocalRandom.current().nextInt(tolls.length);
        long beforeToll = System.currentTimeMillis();
        tolls[randomBooth].pass(vehicle);
        long tollDuration = System.currentTimeMillis() - beforeToll;
        vehicle.addTollTime(tollDuration);

        // 2. Enter waiting area queue
        WaitingArea waitingArea = (startSide == Side.A) ? waitingAreaA : waitingAreaB;
        waitingArea.enter(vehicle);
        ferry.signalVehicleArrived();

        synchronized (vehicle) {
            while (vehicle.getCurrentSide() != destination) {
                vehicle.wait();
            }
        }

        // 3. Signal ferry
        ferry.signalVehicleArrived();

        // 4. Wait until the ferry transports us to destination
        synchronized (vehicle) {
            while (vehicle.getCurrentSide() != destination) {
                vehicle.wait();
            }
        }
        // After being unloaded, compute travel time (boarding → unloading)
        long unloadTime = System.currentTimeMillis();
        long boardingTime = vehicle.getBoardingStartTime();
        if (boardingTime > 0) {
            vehicle.addTravelTime(unloadTime - boardingTime);
        }

        // Record first departure time (the first time we boarded the ferry)
        if (vehicle.getFirstDepartureTime() == 0) {
            vehicle.setFirstDepartureTime(boardingTime);
        }
    }
}