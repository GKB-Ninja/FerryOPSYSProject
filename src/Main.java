import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entry point for the simulation.
 * Initializes common resources, establishes data structures, creates/starts threads,
 * and handles termination upon scenario completion.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Logger.init("simulation.log");

        // Provision 2 toll booths per side to guarantee single-pass exclusivity
        TollBooth[] tollsA = { new TollBooth(Side.A, 1), new TollBooth(Side.A, 2) };
        TollBooth[] tollsB = { new TollBooth(Side.B, 1), new TollBooth(Side.B, 2) };

        WaitingArea waitingAreaA = new WaitingArea(Side.A);
        WaitingArea waitingAreaB = new WaitingArea(Side.B);

        Statistics stats = new Statistics();

        // Start ferry from a random side
        Side startSide = randomSide();
        Ferry ferry = new Ferry(startSide, waitingAreaA, waitingAreaB, stats);

        List<VehicleThread> vehicleThreads = new ArrayList<>();
        List<Vehicle> allVehicles = new ArrayList<>();

        // Initialize entity threads based on constants defined in VehicleType
        createVehicles(VehicleType.CAR, allVehicles, vehicleThreads, tollsA, tollsB, waitingAreaA, waitingAreaB, ferry, stats);
        createVehicles(VehicleType.MINIBUS, allVehicles, vehicleThreads, tollsA, tollsB, waitingAreaA, waitingAreaB, ferry, stats);
        createVehicles(VehicleType.TRUCK, allVehicles, vehicleThreads, tollsA, tollsB, waitingAreaA, waitingAreaB, ferry, stats);

        // Bootstrap execution phase
        FerryThread ferryThread = new FerryThread(ferry);
        ferryThread.start();

        for (VehicleThread vt : vehicleThreads) {
            vt.start();
        }

        // Wait for all vehicles to return to origin before killing ferry (Termination Condition)
        for (VehicleThread vt : vehicleThreads) {
            vt.join();
        }

        // Stop ferry after all vehicles returned
        ferry.stop();
        ferryThread.interrupt();
        ferryThread.join();

        // Report phase
        stats.printTripPerformance();
        stats.printVehiclePerformance(allVehicles);
        stats.printReport();

        Logger.close();
        System.out.println("\nSimulation finished successfully.");
    }

    private static void createVehicles(VehicleType type, List<Vehicle> allVehicles, List<VehicleThread> vehicleThreads,
                                       TollBooth[] tollsA, TollBooth[] tollsB, WaitingArea waitingAreaA, WaitingArea waitingAreaB,
                                       Ferry ferry, Statistics stats) {
        for (int i = 0; i < type.getCount(); i++) {
            // Project Rule: Initialize vehicle to random side
            Vehicle v = new Vehicle(i + 1, type, randomSide());
            allVehicles.add(v);
            vehicleThreads.add(new VehicleThread(v, tollsA, tollsB, waitingAreaA, waitingAreaB, ferry, stats));
        }
    }

    private static Side randomSide() {
        return ThreadLocalRandom.current().nextBoolean() ? Side.A : Side.B;
    }
}