package ac.grim.grimac.checks.impl.analysis.analysisg;

import ac.grim.grimac.utils.math.MathUtil;

public class Rotation {
    private final float yaw;
    private final float pitch;
    private final long timestamp;

    public Rotation(float yaw, float pitch) {
        this.yaw = MathUtil.wrapAngleTo180_float(yaw);
        this.pitch = Math.max(Math.min(pitch, 90.0F), -90.0F);
        this.timestamp = System.currentTimeMillis();
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static float getAngleDifference(float a, float b) {
        return MathUtil.wrapAngleTo180_float(MathUtil.wrapAngleTo180_float(a) - MathUtil.wrapAngleTo180_float(b));
    }
}
