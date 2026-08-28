package cc.watchneko.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.math.MathUtil;

import java.util.LinkedList;
import java.util.Queue;

@CheckData(name = "AimS", configName = "AimS", decay = 0.845)
public final class AimS extends Check implements RotationCheck {
    private final Queue<Double> ratioWindow = new LinkedList<>();
    private int maxBuffer;
    private double minDeltaX;
    private double maxDeltaY;
    private double ratioThreshold;
    private int windowSize;
    private double lastYaw;
    private double lastPitch;
    private long lastTick;

    public AimS(PlayerData player) {
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

        if (Math.abs(currentPitch) == 90
                || player.packetStateData.lastPacketWasTeleport
                || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
            updateRotationData(currentTick, currentYaw, currentPitch);
            ratioWindow.clear();
            return;
        }

        if (player.actionManager.hasAttackedSince(300L)) {
            if (deltaPitch < maxDeltaY && deltaYaw > minDeltaX) {
                double ratio = deltaYaw / (deltaPitch + 0.0001);
                ratioWindow.add(ratio);
                if (ratioWindow.size() > windowSize) {
                    ratioWindow.poll();
                }
                double avgRatio = MathUtil.getAverageDouble(ratioWindow.stream().map(Double::valueOf).toList());
                if (ratioWindow.size() >= windowSize && avgRatio > ratioThreshold) {
                    if (++buffer > maxBuffer && flagAndAlert("avg= " + avgRatio + "\ndy= " + deltaYaw + "\ndp= " + deltaPitch)) {
                        buffer = 0;
                        if (getViolations() > 5) {
                            player.mitigateDamage();
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
