package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.MathUtil;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayDeque;

@CheckData(name = "AimV", configName = "AimV", description = "Detects yaw/pitch deviation imbalance in rotations", decay = 0.65)
public final class AimV extends EdAimCheck {
    private static final float MIN_DELTA = 0.085F;
    private final ArrayDeque<Float> yawSamples = new ArrayDeque<>();
    private final ArrayDeque<Float> pitchSamples = new ArrayDeque<>();
    private int time;

    public AimV(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (player.actionManager.isAttacking()) time = 0;
        if (!hasAttackedSince(500L) ||
                player.getTarget() == null || player.getTarget().type != EntityTypes.PLAYER ||
                Math.abs(update.getTo().getPitch()) >= 89.9F ||
                isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.ELYTRA_FLYING)) {
            return;
        }

        if (time < 5) {
            time++;
            if (time >= 2) {

                final float dy = update.getProcessor().getDeltaYaw() % 360F;
                final float dp = update.getProcessor().getDeltaPitch();

                yawSamples.addLast(dy);
                pitchSamples.addLast(dp);

                while (yawSamples.size() > 16) {
                    yawSamples.removeFirst();
                    pitchSamples.removeFirst();
                }

                if (yawSamples.size() == 16) {
                    double yawSum = 0, yawSq = 0;
                    int yawValid = 0;

                    for (float y : yawSamples) {
                        if (Math.abs(y) < MIN_DELTA) continue;
                        yawSum += y;
                        yawSq += y * y;
                        yawValid++;
                    }

                    double pitchSum = 0, pitchSq = 0;
                    for (float p : pitchSamples) {
                        if (Math.abs(p) < MIN_DELTA) continue;
                        pitchSum += p;
                        pitchSq += p * p;
                    }

                    double yawStd = yawValid > 0 ? MathUtil.stdDev(yawSum, yawSq, yawValid) : 0;
                    double pitchStd = yawValid > 0 ? MathUtil.stdDev(pitchSum, pitchSq, yawValid) : 0;

                    if (yawValid > 8) {
                        if ((yawStd < 0.25F && pitchStd > 2.85F) || (pitchStd < 0.05F && yawStd > 2.45F)) {
                            if (buffer++ > 10) {
                                if (flagAndAlert("ps= " + pitchStd + "\nys= " + yawStd)) {
                                    yawSamples.clear();
                                    pitchSamples.clear();
                                    mitigateDamage();
                                    buffer -= 5;
                                }
                            } else {
                                rewardBufferAndVL();
                            }
                        }
                    }
                }
            }
        }
    }
}
