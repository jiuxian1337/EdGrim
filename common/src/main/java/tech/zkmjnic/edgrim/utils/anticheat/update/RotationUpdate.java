package tech.zkmjnic.edgrim.utils.anticheat.update;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.checks.impl.aim.processor.AimProcessor;
import tech.zkmjnic.edgrim.utils.data.HeadRotation;
import tech.zkmjnic.edgrim.utils.math.Vec2f;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RotationUpdate {
    private HeadRotation from, to;
    private AimProcessor processor;
    private float deltaYRot, deltaXRot;
    private boolean isCinematic;
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
        return EdGrimAPI.INSTANCE.getTickManager().currentTick;
    }
}
