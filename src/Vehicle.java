/**
 * Data model for a given vehicle, holding state, route details, and timing analytics.
 * Synchronizes threads by managing wait/notify conditions based on the vehicle's current side.
 */
public class Vehicle {
    private final int id;
    private final VehicleType type;
    private final Side originSide;
    private volatile Side currentSide;
    private volatile boolean completed = false;

    // Timing metrics for post-simulation statistics
    private long queueEntryTime;
    private long boardingStartTime;
    private long totalWaitTime = 0;
    private long firstDepartureTime = 0;
    private long startTime;
    private long endTime;
    private long totalTollTime = 0;
    private long totalTravelTime = 0;

    public Vehicle(int id, VehicleType type, Side originSide) {
        this.id = id;
        this.type = type;
        this.originSide = originSide;
        this.currentSide = originSide;
    }

    // Getters
    public int getId()              { return id; }
    public VehicleType getType()    { return type; }
    public Side getOriginSide()     { return originSide; }
    public Side getCurrentSide()    { return currentSide; }
    public boolean isCompleted()    { return completed; }
    public long getTotalWaitTime()  { return totalWaitTime; }
    public long getBoardingStartTime() { return boardingStartTime; }
    public long getFirstDepartureTime() { return firstDepartureTime; }
    public long getStartTime()      { return startTime; }
    public long getEndTime()        { return endTime; }
    public long getTotalTollTime()  { return totalTollTime; }
    public long getTotalTravelTime(){ return totalTravelTime; }

    // Setters
    public void setCurrentSide(Side side) { this.currentSide = side; }
    public void setCompleted(boolean done) { this.completed = done; }
    public void setQueueEntryTime(long time)    { this.queueEntryTime = time; }
    public void setBoardingStartTime(long time) { this.boardingStartTime = time; }
    public void addWaitTime(long wait) { this.totalWaitTime += wait; }
    public void setFirstDepartureTime(long time) { this.firstDepartureTime = time; }
    public void setStartTime(long time)        { this.startTime = time; }
    public void setEndTime(long time)          { this.endTime = time; }
    public void addTollTime(long time)         { this.totalTollTime += time; }
    public void addTravelTime(long time)       { this.totalTravelTime += time; }
    public void computeAndAddWaitTime() {
        long wait = boardingStartTime - queueEntryTime;
        if (wait > 0) addWaitTime(wait);
    }

    @Override
    public String toString() {
        return String.format("%s-%d", type, id);
    }
}