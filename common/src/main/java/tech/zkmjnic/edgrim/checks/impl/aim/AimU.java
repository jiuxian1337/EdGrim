package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CheckData(name = "AimU", configName = "AimU", description = "Detects deviations and repeated patterns in predicted rotations", decay = 0.75, setback = 6)
public final class AimU extends EdAimCheck {
    private static final int SAMPLE_SIZE = 100;
    private static final int PATTERN_LENGTH = 3;
    private final List<Vec2f> sample = new ArrayList<>(SAMPLE_SIZE);
    private double buffer2;

    public AimU(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!hasAttackedSince(600L) || isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE)) {
            sample.clear();
            return;
        }

        if (player.getTarget() != null && (player.getTarget().type != EntityTypes.PLAYER || player.getTarget() != player.getLastTarget())) {
            sample.clear();
            return;
        }

        sample.add(new Vec2f(update.getProcessor().getDeltaYaw(), update.getProcessor().getDeltaPitch()));
        if (sample.size() < SAMPLE_SIZE) {
            return;
        }

        boolean flagged = false;
        int filterCount = 0;
        List<Vec2f> patterns = new ArrayList<>();
        for (int i = 1; i < SAMPLE_SIZE; i++) {
            Vec2f prev = sample.get(i - 1);
            Vec2f curr = sample.get(i);
            float absX = Math.abs(curr.getX());
            float diff = Math.abs(Math.abs(curr.getX()) - Math.abs(prev.getY()));
            if (absX > 1.0 && diff < 1e-4) {
                if (++filterCount > 3 && buffer++ >= 3) {
                    flagged = flagAndAlert("(Filter)\np= " + diff);
                    if (flagged) {
                        mitigateDamage();
                        rewardBufferAndVL();
                    }
                }
            }
            if (!flagged) {
                if (i <= SAMPLE_SIZE - PATTERN_LENGTH) {
                    for (int j = i + PATTERN_LENGTH; j <= SAMPLE_SIZE - PATTERN_LENGTH; j++) {
                        for (int k = 0; k < PATTERN_LENGTH; k++) {
                            Vec2f a = sample.get(i + k);
                            Vec2f b = sample.get(j + k);
                            if (Objects.equals(a, b) && !patterns.contains(a)) {
                                patterns.add(a);
                            }
                        }
                    }
                }
            }
        }

        if (!flagged) {
            for (Vec2f vec : patterns) {
                float x = Math.abs(vec.getX());
                float y = Math.abs(vec.getY());
                if ((x > 1.0 || y > 1.0) && x > 0.26 && y > 0.26) {
                    if (buffer2++ > 6) {
                        if (flagAndAlert("(Offset)\np= " + vec)) {
                            mitigateDamage();
                            rewardBufferAndVL();
                        }
                        break;
                    }
                }
            }
        }

        if (!flagged) {
            buffer2 = Math.max(0, buffer2 - getDecay());
            if (buffer2 == 0) rewardVL();
        }
        sample.clear();
    }
}
