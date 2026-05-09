package tech.zkmjnic.edgrim.utils.math;

public record Vec2d(double x, double y) {

    public Vec2d(Number x, Number y) {
        this(x.doubleValue(), y.doubleValue());
    }
}
