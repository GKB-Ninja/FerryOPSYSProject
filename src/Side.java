public enum Side {
    A, B;

    public Side getOpposite() {
        return this == A ? B : A;
    }

    @Override
    public String toString() {
        return this == A ? "A" : "B";
    }
}