package cc.watchneko.utils.math;

public final class Simplification {
    public static float castTo360(float num) {
        return Math.abs((num + 360) % 360 - 180);
    }

    public static double scaleVal(double value, double scale) {
        double scale2 = Math.pow(10, scale);
        return Math.ceil(value * scale2) / scale2;
    }
}
