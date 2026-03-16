package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@CheckData(name = "AimW", description = "Self-adaptive detection of mechanical aim rotation patterns", configName = "AimW", decay = 0.82)
public final class AimW extends EdAimCheck {
    private final List<Vec2f> rawRotations = new CopyOnWriteArrayList<>();
    private final List<Vec2f> rotations = new CopyOnWriteArrayList<>();
    private double lastAverageX = 0.0;
    private double lastAverageY = 0.0;

    public AimW(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(@NotNull RotationUpdate u) {
        if (!hasAttackedSince(1000)) {
            rawRotations.clear();
            rotations.clear();
            return;
        }
        if (player.getTarget() == null || player.getTarget().type != EntityTypes.PLAYER) {
            rawRotations.clear();
            rotations.clear();
            return;
        }

        if (player.getTarget() != player.getLastTarget()) {
            rawRotations.clear();
            rotations.clear();
            return;
        }

        if (Math.abs(u.getTo().getPitch()) >= 89.9F) return;
        if (!isMoving()) return;
        if (player.getTarget().getPossibleLocationBoxes().distance(player.getBoundingBox()) <= 0.5) return;
        if (isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.ELYTRA_FLYING)) return;

        final float pitchAccel = u.getProcessor().getPitchAccel();
        final float yawAccel = u.getProcessor().getYawAccel();
        rawRotations.add(new Vec2f(pitchAccel, yawAccel));
        rotations.add(new Vec2f(u.getTo().getPitch(), u.getTo().getYaw()));

        if (rawRotations.size() > 15 && rotations.size() > 20) {
            checkRaw(u);
        }
    }

    private void checkRaw(RotationUpdate update) {
        if (update.isCinematic()) {
            rawRotations.clear();
            rotations.clear();
            return;
        }
        List<Float> xList = new ArrayList<>();
        List<Float> yList = new ArrayList<>();
        for (Vec2f rot : rotations) {
            xList.add(rot.getX());
            yList.add(rot.getY());
        }

        List<Float> xAccelList = new ArrayList<>();
        List<Float> yAccelList = new ArrayList<>();
        for (Vec2f vec : rawRotations) {
            xAccelList.add(vec.getX());
            yAccelList.add(vec.getY());
        }

        final double avgYaw = MathUtil.getAverage(xList);
        final double avgPitch = MathUtil.getAverage(yList);
        final double longAvgYaw = update.getProcessor().getAvgYaw();
        final double longAvgPitch = update.getProcessor().getAvgPitch();

        final int sens = calculateSensitivity();
        final int sensTemp = update.getProcessor().totalSensitivityClient;
        final double averageX = MathUtil.getAverage(xAccelList);
        final double averageY = MathUtil.getAverage(yAccelList);
        final double entropyX = MathUtil.calculateEntropy(xList, 5);
        final double entropyY = MathUtil.calculateEntropy(yList, 8);
        final double nEntropyX = MathUtil.calculateNEntropy(xList);
        final double nEntropyY = MathUtil.calculateNEntropy(yList);
        final double varianceX = MathUtil.getVariance(xAccelList);
        final double varianceY = MathUtil.getVariance(yAccelList);
        final double consistencyX = MathUtil.calculatePatternConsistency(xAccelList);
        final double consistencyY = MathUtil.calculatePatternConsistency(yAccelList);
        final double magnitude = Math.sqrt(averageX * averageX + averageY * averageY);
        final double deltaX = Math.abs(averageX - lastAverageX);
        final double deltaY = Math.abs(averageY - lastAverageY);
        if (sens > 50) {

        }
        lastAverageX = averageX;
        lastAverageY = averageY;
    }
}
