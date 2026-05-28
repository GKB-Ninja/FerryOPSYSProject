import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) throws Exception {
        Logger.init("simulation.log");

        TollBooth[] tollsA = { new TollBooth(Side.A, 1), new TollBooth(Side.A, 2) };
        TollBooth[] tollsB = { new TollBooth(Side.B, 1), new TollBooth(Side.B, 2) };

        WaitingArea waitingAreaA = new WaitingArea(Side.A);
        WaitingArea waitingAreaB = new WaitingArea(Side.B);

        Statistics stats = new Statistics();

        Side startSide = ThreadLocalRandom.current().nextBoolean() ? Side.A : Side.B;
        Ferry ferry = new Ferry(startSide, waitingAreaA, waitingAreaB, stats);

        List<VehicleThread> vehicleThreads = new ArrayList<>();
        List<Vehicle> allVehicles = new ArrayList<>();   // store vehicles for statistics

        // Create vehicles
        for (int i = 0; i < VehicleType.CAR.getCount(); i++) {
            Vehicle v = new Vehicle(i+1, VehicleType.CAR, randomSide());
            allVehicles.add(v);
            vehicleThreads.add(new VehicleThread(v, tollsA, tollsB,
                    waitingAreaA, waitingAreaB, ferry, stats));
        }

        for (int i = 0; i < VehicleType.MINIBUS.getCount(); i++) {
            Vehicle v = new Vehicle(i+1, VehicleType.MINIBUS, randomSide());
            allVehicles.add(v);
            vehicleThreads.add(new VehicleThread(v, tollsA, tollsB,
                    waitingAreaA, waitingAreaB, ferry, stats));
        }

        for (int i = 0; i < VehicleType.TRUCK.getCount(); i++) {
            Vehicle v = new Vehicle(i+1, VehicleType.TRUCK, randomSide());
            allVehicles.add(v);
            vehicleThreads.add(new VehicleThread(v, tollsA, tollsB,
                    waitingAreaA, waitingAreaB, ferry, stats));
        }

        // Start ferry and vehicles
        FerryThread ferryThread = new FerryThread(ferry);
        ferryThread.start();
        for (VehicleThread vt : vehicleThreads) {
            vt.start();
        }

        // Wait for all vehicles
        for (VehicleThread vt : vehicleThreads) {
            vt.join();
        }

        // Stop ferry after all vehicles returned.
        ferry.stop();
        ferryThread.interrupt();
        ferryThread.join();

        // Print detailed vehicle performance table
        stats.printTripPerformance();
        stats.printVehiclePerformance(allVehicles);
        stats.printReport();
        Logger.close();

        System.out.println("\nSimulation finished successfully.");
    }

    private static Side randomSide() {
        return ThreadLocalRandom.current().nextBoolean() ? Side.A : Side.B;
    }
}