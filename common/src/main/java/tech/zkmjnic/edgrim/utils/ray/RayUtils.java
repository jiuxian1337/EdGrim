package tech.zkmjnic.edgrim.utils.ray;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RayUtils {
    private static final boolean[] BOOLEANS = {true, false};

    public static double scaleVal(double value, double scale) {
        double scale2 = Math.pow(10, scale);
        return Math.ceil(value * scale2) / scale2;
    }

    public static float wrapYaw(float yaw) {
        float yR = (float) Math.toRadians(yaw);
        RayLine a = new RayLine(-Math.sin(yR), Math.cos(yR));
        return (float) calculateRayLine(a);
    }

    public static boolean validRayLines(RayLine a, RayLine b, double radi) {
        double angleA = Math.atan2(a.z(), a.x());
        double angleB = Math.atan2(b.z(), b.x());

        double angleDiff = Math.abs(angleA - angleB);

        if (angleDiff > Math.PI) {
            angleDiff = 2 * Math.PI - angleDiff;
        }

        return angleDiff <= Math.toRadians(radi);
    }

    public static double calculateRayLines(RayLine a, RayLine b) {
        double angleA = Math.atan2(a.z(), a.x());
        double angleB = Math.atan2(b.z(), b.x());

        double angleDiff = Math.abs(angleA - angleB);

        if (angleDiff > Math.PI) {
            angleDiff = 2 * Math.PI - angleDiff;
        }

        return Math.toDegrees(angleDiff);
    }

    public static double calculateRayLine(RayLine a) {
        return Math.atan2(a.z(), a.x());
    }

    public static float masterCast(float num) {
        return sinusoidOnlyMin(castTo360(num));
    }

    public static float castTo360(float num) {
        // int wrapShitMath = (int) Math.floor(value / 360);
        return Math.abs((num + 360) % 360 - 180);
    }

    public static float castTo360WithLimit(float num) {
        float value = Math.abs((num + 360) % 360 - 180);
        return set360Limit(value);
    }

    public static float sinusoidOnlyMin(float num) {
        int wrapShitMath = (int) Math.floor(num / 180);
        return num - (wrapShitMath * 180);
    }

    public static float set360Limit(float num) {
        int wrapShitMath = (int) Math.floor(num / 360);
        return num - (wrapShitMath * 360);
    }

    private static float[] getRotation(Player player1, Entity entity2) {
        Vector direction = entity2.getLocation().toVector().subtract(player1.getLocation().toVector());
        float yaw = (float) Math.toDegrees(Math.atan2(direction.getX(), direction.getZ())),
                pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));
        return new float[]{castTo360(yaw), pitch};
    }

    public static boolean onBound(Location point, Location target, double x, double y, double z) {
        boolean xB = (point.getX() >= (target.getX() - (x / 2)) && point.getX() <= (target.getX() + (x / 2)));
        boolean yB = (point.getY() >= (target.getY() - (y / 2)) && point.getY() <= (target.getY() + (y / 2)));
        boolean zB = (point.getZ() >= (target.getZ() - (z / 2)) && point.getZ() <= (target.getZ() + (z / 2)));
        return xB && yB && zB;
    }
}
