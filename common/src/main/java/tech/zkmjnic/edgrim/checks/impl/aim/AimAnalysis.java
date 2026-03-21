package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CheckData(name = "AimAnalysis", configName = "AimAnalysis", decay = 0.75, description = "aim analysis migrated")
public final class AimAnalysis extends Check implements RotationCheck {
    private static final int SAMPLE_SIZE = 100;

    private final List<Float> rawYaw = new ArrayList<>(SAMPLE_SIZE);
    private final List<Float> rawPitch = new ArrayList<>(SAMPLE_SIZE);

    private final List<Float> limitedYaw = new ArrayList<>(SAMPLE_SIZE);
    private final List<Float> limitedPitch = new ArrayList<>(SAMPLE_SIZE);

    private final List<Float> longTermAnalysis = new ArrayList<>(10);
    private boolean linearQuery;

    private boolean linearEnabled = true;
    private boolean longtermEnabled = true;

    public AimAnalysis(PlayerData player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        linearEnabled = config.getBooleanElse("AimAnalysis.enabled-linear", true);
        longtermEnabled = config.getBooleanElse("AimAnalysis.enabled-longterm", true);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(2500L)) {
            limitedYaw.clear();
            limitedPitch.clear();
            return;
        }
        if (rotationUpdate.isCinematic()) return;

        if (player.packetStateData.lastPacketWasTeleport
                || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
//            rawYaw.clear();
//            rawPitch.clear();
//            limitedYaw.clear();
//            limitedPitch.clear();
            return;
        }

        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();

        rawYaw.add(deltaYaw);
        rawPitch.add(deltaPitch);
        if (rawYaw.size() >= SAMPLE_SIZE) {
            checkRaw();
            rawYaw.remove(0);
            rawPitch.remove(0);
        }

        if (Math.abs(deltaYaw) > 1.35f || (Math.abs(deltaPitch) > 1.35f && Math.abs(deltaYaw) > 0.32f)) {
            limitedYaw.add(deltaYaw);
            limitedPitch.add(deltaPitch);
            if (limitedYaw.size() >= SAMPLE_SIZE) {
                checkLimited();
            }
        }
    }

    private void checkLimited() {
        int resultDistinct = 0;
        final List<Float> yawStack = new ArrayList<>(10);
        for (final float yaw : limitedYaw) {
            yawStack.add(yaw);
            if (yawStack.size() >= 10) {
                resultDistinct += MathUtil.getDistinct(MathUtil.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        final float distinctRank = (float) resultDistinct / 60.0f;
        longTermAnalysis.add(distinctRank);

        if (longTermAnalysis.size() >= 10) {
            final double avg = MathUtil.getAverage(longTermAnalysis);
            double normal = 0;
            for (double d : longTermAnalysis) {
                if (d > 0.97) normal++;
            }

            if (avg < 0.95 && normal < 4 && longtermEnabled) {
                flagAnalysis("t=LongTerm avg=" + avg + " normal=" + normal + "/10");
            }
            longTermAnalysis.clear();
        }

        limitedYaw.clear();
        limitedPitch.clear();
    }

    private void checkRaw() {
        final int sens = player.calculateSensitivity();
        final List<Float> yawStack = new ArrayList<>(10);
        final List<Double> resultDeviation = new ArrayList<>();
        int resultDistinct = 0;

        for (final float yaw : rawYaw) {
            yawStack.add(yaw);
            if (yawStack.size() >= 10) {
                resultDeviation.add(MathUtil.getStandardDeviation(MathUtil.getJiffDelta(yawStack, 5)));
                resultDistinct += MathUtil.getDistinct(MathUtil.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        final List<Double> outliers = Statistics.getZScoreOutliers(resultDeviation, 0.5);

        if (outliers.isEmpty() || (outliers.size() == 1 && Math.abs(outliers.get(0)) > 10 && Math.abs(outliers.get(0)) < 100)) {
            if (!linearQuery) {
                linearQuery = true;
            } else if (linearEnabled) {
                flagAnalysis("t=Linear outliers=" + Arrays.toString(outliers.toArray()));
            }
        } else {
            linearQuery = false;
        }
    }

    private boolean flagAnalysis(String verbose) {
        if (flagAndAlert(verbose)) {
            player.mitigateDamage();
            return true;
        }
        return false;
    }
}
