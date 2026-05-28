/**
 * Defines the parameters for different simulation actors.
 * Stores spatial cost (capacity) and the total population count (count).
 */
public enum VehicleType {
    CAR(1, 12),
    MINIBUS(2, 10),
    TRUCK(3, 8);

    private final int capacity;
    private final int count;

    VehicleType(int capacity, int count) {
        this.capacity = capacity;
        this.count = count;
    }

    public int getCapacity() { return capacity; }
    public int getCount()    { return count; }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ENGLISH);
    }
}