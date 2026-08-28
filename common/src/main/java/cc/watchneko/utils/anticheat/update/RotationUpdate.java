package cc.watchneko.utils.anticheat.update;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.checks.impl.aim.processor.AimProcessor;
import cc.watchneko.utils.data.HeadRotation;
import cc.watchneko.utils.math.Vec2f;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RotationUpdate {
    private HeadRotation from, to;
    private AimProcessor processor;
    private float deltaYRot, deltaXRot;
    private boolean isCinematic;
    private boolean isCinematic2;
    private double sensitivityX, sensitivityY;

    public RotationUpdate(HeadRotation from, HeadRotation to, float deltaXRot, float deltaYRot) {
        this.from = from;
        this.to = to;
        this.deltaXRot = deltaXRot;
        this.deltaYRot = deltaYRot;
    }

    public float getDeltaXRotABS() {
        return Math.abs(deltaXRot);
    }

    public float getDeltaYRotABS() {
        return Math.abs(deltaYRot);
    }

    public Vec2f getDelta() {
        return new Vec2f(to.getYaw() - from.getYaw(), to.getPitch() - from.getPitch());
    }

    public long getTick() {
        return WatchNekoAPI.INSTANCE.getTickManager().currentTick;
    }
}
