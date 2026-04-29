package tech.zkmjnic.edgrim.checks.impl.aim;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.ArrayDeque;

@CheckData(
        name = "AimC",
        configName = "AimC",
        description = "Simplified rotation analysis",
        decay = 0.65
)
public final class AimC extends Check implements RotationCheck {
    private static final float MIN_DELTA = 0.085F;
    private final ArrayDeque<Float> yawSamples = new ArrayDeque<>();
    private final ArrayDeque<Float> pitchSamples = new ArrayDeque<>();
    private int time;

    public AimC(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (player.actionManager.isAttacking()) {
            time = 0;
        }
        if (!player.actionManager.hasAttackedSince(500L)
                || player.getTarget() == null
                || player.getTarget().type != EntityTypes.PLAYER
                || Math.abs(update.getTo().getPitch()) >= 89.9F
                || player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.horseInteractCausedForcedRotation) {
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
                    double yawSum = 0;
                    double yawSq = 0;
                    int yawValid = 0;
                    for (float y : yawSamples) {
                        if (Math.abs(y) < MIN_DELTA) continue;
                        yawSum += y;
                        yawSq += y * y;
                        yawValid++;
                    }

                    double pitchSum = 0;
                    double pitchSq = 0;
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
                                    player.mitigateDamage();
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
