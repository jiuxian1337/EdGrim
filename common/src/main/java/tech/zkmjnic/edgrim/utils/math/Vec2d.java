package tech.zkmjnic.edgrim.utils.math;

public final class Vec2d {
    private final double x;
    private final double y;

    public Vec2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2d(Number x, Number y) {
        this(x.doubleValue(), y.doubleValue());
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
