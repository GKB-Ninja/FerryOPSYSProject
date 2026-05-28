import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe tracker for simulation metrics.
 * Calculates global wait times, trip counts, and prints formatted end-of-simulation reports.
 */
public class Statistics {
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    private final AtomicLong maxWaitTime = new AtomicLong(0);
    private final AtomicInteger completedVehicles = new AtomicInteger(0);
    private final AtomicInteger totalTrips = new AtomicInteger(0);
    private final AtomicLong totalFerryTravelTime = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    // Store trip records for later printing
    private final List<TripRecord> tripRecords = new ArrayList<>();

    private static class TripRecord {
        final int tripNumber;
        final Side side;
        final int load;
        final int vehicleCount;
        final long waitMs;
        final long travelMs;

        TripRecord(int tripNumber, Side side, int load, int vehicleCount, long waitMs, long travelMs) {
            this.tripNumber = tripNumber;
            this.side = side;
            this.load = load;
            this.vehicleCount = vehicleCount;
            this.waitMs = waitMs;
            this.travelMs = travelMs;
        }
    }

    public void addWaitTime(long waitTime) {
        totalWaitTime.addAndGet(waitTime);
        maxWaitTime.updateAndGet(current -> Math.max(current, waitTime));
    }

    public void recordVehicleCompleted() {
        completedVehicles.incrementAndGet();
    }

    public void recordTrip(Side side, int load, int vehicleCount, long waitMs, long travelMs) {
        int tripNum = totalTrips.incrementAndGet();
        totalFerryTravelTime.addAndGet(travelMs);
        tripRecords.add(new TripRecord(tripNum, side, load, vehicleCount, waitMs, travelMs));
    }

    public void printTripPerformance() {
        if (tripRecords.isEmpty()) {
            System.out.println("No ferry trips recorded.");
            return;
        }
        System.out.println("========================= FERRY STATS =========================");
        System.out.printf("%-6s %-5s %-10s %-12s %-10s %-12s %-10s%n",
                "Trip", "Side", "Vehicles", "Load Units", "Load %", "Wait(s)", "Travel(s)");
        System.out.println("===============================================================");
        for (TripRecord t : tripRecords) {
            double loadPct = (t.load * 100.0) / 20.0;
            System.out.printf("%-6d %-5s %-10d %-12d %-10.1f %-12.3f %-10.3f%n",
                    t.tripNumber, t.side.toString(),
                    t.vehicleCount, t.load, loadPct,
                    t.waitMs / 1000.0, t.travelMs / 1000.0);
        }
        System.out.println("===============================================================");
    }

    public void printVehiclePerformance(List<Vehicle> vehicles) {
        System.out.println("\n=================================================== PERFORMANCE PER VEHICLE ===========================================================");
        System.out.printf("%-10s %-5s %-10s %-15s %-15s %-17s %-17s %-15s %-15s %-15s%n",
                "Type", "ID", "Start Side", "Idle(s)", "Travel(s)", "First Dep Time(s)", "End Time(s)",
                "Queue Wait(s)", "Toll Wait(s)", "Total Time(s)");
        System.out.println("=======================================================================================================================================");

        List<Vehicle> sorted = new ArrayList<>(vehicles);
        sorted.sort(Comparator.comparing(Vehicle::getType).thenComparingInt(Vehicle::getId));

        long simStart = this.startTime;
        double totalCarTotal = 0, totalCarToll = 0, totalCarQueue = 0, totalCarTravel = 0, totalCarIdle = 0;
        double totalCarDepart = 0, totalCarReturn = 0;
        int carCount = 0;

        double totalMinibusTotal = 0, totalMinibusToll = 0, totalMinibusQueue = 0, totalMinibusTravel = 0, totalMinibusIdle = 0;
        double totalMinibusDepart = 0, totalMinibusReturn = 0;
        int minibusCount = 0;

        double totalTruckTotal = 0, totalTruckToll = 0, totalTruckQueue = 0, totalTruckTravel = 0, totalTruckIdle = 0;
        double totalTruckDepart = 0, totalTruckReturn = 0;
        int truckCount = 0;

        for (Vehicle v : sorted) {
            long total = v.getEndTime() - v.getStartTime();
            long toll = v.getTotalTollTime();
            long queue = v.getTotalWaitTime();
            long travel = v.getTotalTravelTime();
            long idle = total - toll - queue - travel;

            double totalSec = total / 1000.0;
            double tollSec = toll / 1000.0;
            double queueSec = queue / 1000.0;
            double travelSec = travel / 1000.0;
            double idleSec = idle / 1000.0;

            double depSec = (v.getFirstDepartureTime() - simStart) / 1000.0;
            double retSec = (v.getEndTime() - simStart) / 1000.0;

            System.out.printf("%-10s %-5d %-10s %-15.2f %-15.2f %-17.3f %-17.3f %-15.2f %-15.2f %-15.2f%n",
                    v.getType().toString(), v.getId(),
                    v.getOriginSide().toString(),
                    idleSec, travelSec, depSec, retSec,
                    queueSec, tollSec, totalSec);

            switch (v.getType()) {
                case CAR:
                    totalCarTotal += totalSec; totalCarToll += tollSec; totalCarQueue += queueSec;
                    totalCarTravel += travelSec; totalCarIdle += idleSec;
                    totalCarDepart += depSec; totalCarReturn += retSec;
                    carCount++;
                    break;
                case MINIBUS:
                    totalMinibusTotal += totalSec; totalMinibusToll += tollSec; totalMinibusQueue += queueSec;
                    totalMinibusTravel += travelSec; totalMinibusIdle += idleSec;
                    totalMinibusDepart += depSec; totalMinibusReturn += retSec;
                    minibusCount++;
                    break;
                case TRUCK:
                    totalTruckTotal += totalSec; totalTruckToll += tollSec; totalTruckQueue += queueSec;
                    totalTruckTravel += travelSec; totalTruckIdle += idleSec;
                    totalTruckDepart += depSec; totalTruckReturn += retSec;
                    truckCount++;
                    break;
            }
        }

        System.out.println("====================================================== AVERAGE STATS PER VEHICLE ======================================================");

        printAvgLine("Car", "-", carCount, totalCarIdle, totalCarTravel, totalCarDepart, totalCarReturn, totalCarQueue, totalCarToll, totalCarTotal);
        printAvgLine("Minibus", "-", minibusCount, totalMinibusIdle, totalMinibusTravel, totalMinibusDepart, totalMinibusReturn, totalMinibusQueue, totalMinibusToll, totalMinibusTotal);
        printAvgLine("Truck", "-", truckCount, totalTruckIdle, totalTruckTravel, totalTruckDepart, totalTruckReturn, totalTruckQueue, totalTruckToll, totalTruckTotal);

        int overallCount = carCount + minibusCount + truckCount;
        double overallIdle = totalCarIdle + totalMinibusIdle + totalTruckIdle;
        double overallTravel = totalCarTravel + totalMinibusTravel + totalTruckTravel;
        double overallQueue = totalCarQueue + totalMinibusQueue + totalTruckQueue;
        double overallToll = totalCarToll + totalMinibusToll + totalTruckToll;
        double overallTotal = totalCarTotal + totalMinibusTotal + totalTruckTotal;
        double overallDepart = totalCarDepart + totalMinibusDepart + totalTruckDepart;
        double overallReturn = totalCarReturn + totalMinibusReturn + totalTruckReturn;

        printAvgLine("OVERALL", "-", overallCount, overallIdle, overallTravel, overallDepart, overallReturn, overallQueue, overallToll, overallTotal);
        System.out.println("========================================================================================================================================");
    }

    private void printAvgLine(String type, String label, int count,
                              double sumIdle, double sumTravel,
                              double sumDepart, double sumReturn,
                              double sumQueue, double sumToll, double sumTotal) {
        if (count > 0) {
            System.out.printf("%-10s %-5s %-10s %-15.2f %-15.2f %-17.3f %-17.3f %-15.2f %-15.2f %-15.2f%n",
                    type, label, "-",
                    sumIdle / count, sumTravel / count,
                    sumDepart / count, sumReturn / count,
                    sumQueue / count, sumToll / count, sumTotal / count);
        } else {
            System.out.printf("%-10s %-5s %-10s %-15s %-15s %-17s %-17s %-15s %-15s %-15s%n",
                    type, label, "-", "-", "-", "-", "-", "-", "-", "-");
        }
    }

    public void printReport() {
        long totalTime = System.currentTimeMillis() - startTime;
        int completed = completedVehicles.get();

        System.out.println("\n========== SIMULATION STATISTICS ==========");
        System.out.printf("Total simulation time: %.2f seconds%n", totalTime / 1000.0);

        if (completed > 0) {
            System.out.printf("Average waiting time per vehicle: %.2f ms%n", (double) totalWaitTime.get() / completed);
            System.out.printf("Maximum waiting time: %d ms%n", maxWaitTime.get());
        } else {
            System.out.println("No vehicles completed.");
        }

        System.out.printf("Number of ferry trips: %d%n", totalTrips.get());

        // Utilize the dynamic random travel time to calculate accurate utilization ratio
        long travelTime = totalFerryTravelTime.get();
        double utilization = (totalTime > 0) ? (100.0 * travelTime / totalTime) : 0.0;
        System.out.printf("Ferry utilization ratio: %.2f%%%n", utilization);
        System.out.println("===========================================");
    }
}