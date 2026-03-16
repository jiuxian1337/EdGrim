package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.LinkedList;
import java.util.Queue;

@CheckData(name = "AimR", configName = "AimR", decay = 0.845, description = "Detects abnormal yaw-to-pitch ratio spikes")
public final class AimR extends EdAimCheck {
    private final Queue<Double> ratioWindow = new LinkedList<>();
    private int maxBuffer;
    private double minDeltaX;
    private double maxDeltaY;
    private double ratioThreshold;
    private int windowSize;
    private double lastYaw;
    private double lastPitch;
    private long lastTick;

    public AimR(EdGrimPlayer player) {
        super(player);
        lastTick = -1;
    }

    @Override
    public void process(final RotationUpdate update) {
        long currentTick = update.getTick();

        if (lastTick == -1) {
            updateRotationData(currentTick, update.getTo().getYaw(), update.getTo().getPitch());
            return;
        }

        if (currentTick == lastTick) {
            return;
        }

        double currentYaw = update.getTo().getYaw();
        double currentPitch = update.getTo().getPitch();

        double deltaYaw = Math.abs(currentYaw - lastYaw);

        double deltaPitch = Math.abs(currentPitch - lastPitch);

        if (Math.abs(currentPitch) == 90) {
            updateRotationData(currentTick, currentYaw, currentPitch);
            ratioWindow.clear();
            return;
        }

        if (isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE)) {
            updateRotationData(currentTick, currentYaw, currentPitch);
            ratioWindow.clear();
            return;
        }

        if (hasAttackedSince(300L)) {
            if (deltaPitch < maxDeltaY && deltaYaw > minDeltaX) {
                double ratio = deltaYaw / (deltaPitch + 0.0001);
                ratioWindow.add(ratio);
                if (ratioWindow.size() > windowSize) {
                    ratioWindow.poll();
                }
                double avgRatio = MathUtil.getAverage(ratioWindow);
                if (ratioWindow.size() >= windowSize && avgRatio > ratioThreshold) {
                    if (++buffer > maxBuffer) {
                        if (flagAndAlert("avg= " + avgRatio +
                                "\ndy= " + deltaYaw +
                                "\ndp= " + deltaPitch)) {
                            buffer = 0;
                            if (violations > 5) mitigateDamage();
                        }
                    }
                }
            } else {
                ratioWindow.clear();
                buffer = Math.max(buffer - 0.5, 0);
            }
        } else {
            ratioWindow.clear();
            rewardBufferAndVL();
        }

        updateRotationData(currentTick, currentYaw, currentPitch);
    }

    private void updateRotationData(long tick, double yaw, double pitch) {
        lastTick = tick;
        lastYaw = yaw;
        lastPitch = pitch;
    }

    @Override
    public void onReload(ConfigManager config) {
        maxBuffer = config.getIntElse(getConfigName() + ".buffer", 7);
        minDeltaX = config.getDoubleElse(getConfigName() + ".min-deltaX", 1.0D);
        maxDeltaY = config.getDoubleElse(getConfigName() + ".max-deltaY", 0.0001D);
        ratioThreshold = config.getDoubleElse(getConfigName() + ".ratio-threshold", 1000.0D);
        windowSize = config.getIntElse(getConfigName() + ".window-size", 5);
    }
}
