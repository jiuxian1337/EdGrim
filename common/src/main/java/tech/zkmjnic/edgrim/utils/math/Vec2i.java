package tech.zkmjnic.edgrim.utils.math;

public record Vec2i(int x, int y) {

    public Vec2i(Number x, Number y) {
        this(x.intValue(), y.intValue());
    }
}
