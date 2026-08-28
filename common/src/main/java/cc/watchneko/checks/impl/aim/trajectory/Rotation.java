package cc.watchneko.checks.impl.aim.trajectory;

import cc.watchneko.utils.math.MathUtil;

public class Rotation {
    private final float yaw;
    private final float pitch;
    private final long timestamp;

    public Rotation(float yaw, float pitch) {
        this.yaw = MathUtil.wrapAngleTo180_float(yaw);
        this.pitch = Math.max(Math.min(pitch, 90), -90);
        this.timestamp = System.currentTimeMillis();
    }

    public static float getAngleDifference(final float a, final float b) {
        return MathUtil.wrapAngleTo180_float(MathUtil.wrapAngleTo180_float(a) - MathUtil.wrapAngleTo180_float(b));
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
}
