package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.math.Vec2f;
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
        return GrimAPI.INSTANCE.getTickManager().currentTick;
    }
}
