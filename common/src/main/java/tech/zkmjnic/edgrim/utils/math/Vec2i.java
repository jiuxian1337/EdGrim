package tech.zkmjnic.edgrim.utils.math;

public final class Vec2i {
    private final int x;
    private final int y;

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vec2i(Number x, Number y) {
        this(x.intValue(), y.intValue());
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
