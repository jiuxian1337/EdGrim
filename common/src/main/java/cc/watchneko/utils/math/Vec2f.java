package cc.watchneko.utils.math;

public record Vec2f(float x, float y) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec2f vec2f = (Vec2f) o;
        return Float.compare(vec2f.x, x) == 0 && Float.compare(vec2f.y, y) == 0;
    }

    @Override
    public String toString() {
        return "Vec2f{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
