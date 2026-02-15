package ac.grim.grimac.checks.impl.analysis;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.aim.ExemptType;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.analysis.analysisf.BasicModuleConfig;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.MathUtil;
import ac.grim.grimac.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CheckData(name = "AnalysisF", configName = "AnalysisF", decay = 0.75, description = "Rotation module analysis")
public final class AnalysisF extends AnalysisCheck implements RotationCheck {
    private final List<Vec2f> rawRotations = new ArrayList<>();
    private Map<ModuleType, BasicModuleConfig> configs;
    private Map<ModuleType, Double> buffers;

    public AnalysisF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!hasAttackedSince(500L)) {
            buffer *= 0.95;
            return;
        }

        if (player.getTarget() == null) {
            return;
        }

        if (hasExemptions()) {
            buffer *= 0.85;
        }

        if (rotationUpdate.isCinematic()) {
            return;
        }

        AimProcessor processor = rotationUpdate.getProcessor();
        if (processor == null) {
            return;
        }

        if (Math.abs(processor.getPitch()) >= 90F) {
            return;
        }

        int machineKnownMovement = 0;
        int robotizedAmount = 0;
        int infinitives = 0;
        int gcd = 0;

        rawRotations.add(new Vec2f(player.getYaw(), player.getPitch()));
        if (rawRotations.size() < 20) {
            return;
        }

        Set<Double> yaws = new HashSet<>();
        double oldYaw = rawRotations.get(0).getX();
        for (Vec2f r : rawRotations) {
            yaws.add(Math.abs(r.getX() - oldYaw));
            oldYaw = r.getX();
        }

        double oldYawResult = rawRotations.get(0).getX();
        double oldPitchResult = rawRotations.get(0).getY();
        double yawChangeFirst = Math.abs(rawRotations.get(0).getX() - rawRotations.get(1).getX());

        for (Vec2f rotation : rawRotations) {
            double yawChange = Math.abs(rotation.getX() - oldYawResult);
            double pitchChange = Math.abs(rotation.getY() - oldPitchResult);
            double robotized = Math.abs(yawChange - yawChangeFirst);
            double interpolation = MathUtil.scaleVal(yawChange / robotized, 2);

            if (MathUtil.scaleVal(yawChange, 2.0) == 0.1 || MathUtil.scaleVal(pitchChange, 2.0) == 0.1) {
                gcd++;
            }
            if (MathUtil.scaleVal(yawChange, 2.0) == 0.01 || MathUtil.scaleVal(pitchChange, 2.0) == 0.01) {
                gcd++;
            }
            if (robotized < 2 && yawChange > 2.5) {
                robotizedAmount++;
            }
            if (robotized < 0.99 && yawChange > 4) {
                machineKnownMovement++;
            }
            if (Double.isInfinite(interpolation) && yawChange > 0) {
                infinitives++;
                if (infinitives > 1 && yawChange < 0.4) {
                    infinitives--;
                }
            }

            oldYawResult = rotation.getX();
            oldPitchResult = rotation.getY();
        }

        int sens = player.calculateSensitivity();
        if (sens > 65 && sens < 90) {
            if (gcd > 2) {
                handleCheck(ModuleType.GCD, "(Pattern)\ng= " + gcd, this::mitigateDamage);
            }
            if (robotizedAmount >= 10 && Math.abs(MathUtil.getAverage(yaws)) > 2.5) {
                handleCheck(ModuleType.SYNC, "(Sync)\nr= " + robotizedAmount + "\ns= " + sens, this::mitigateDamage);
            }
            if (machineKnownMovement > 9 && Math.abs(MathUtil.getAverage(yaws)) > 3.0) {
                handleCheck(ModuleType.MACHINE, "(Normal)\nm= " + machineKnownMovement + "\ns= " + sens, this::mitigateDamage);
            }
        } else if (sens < 65) {
            if (machineKnownMovement > 8) {
                handleCheck(ModuleType.OUTSENS_MACHINE, "(Machine)\nm= " + machineKnownMovement + "\ns= " + sens, this::mitigateDamage);
            }
            if (infinitives > 1 && Math.abs(MathUtil.getAverage(yaws)) > 3.2) {
                handleCheck(ModuleType.OUTSENS_INTERPOLATION, "(Interpolation)\ni= " + infinitives + "\ns= " + sens, this::mitigateDamage);
            }
            rewardBufferAndVL();
        }

        rawRotations.clear();
    }

    @Override
    public void onReload(ConfigManager config) {
        loadModuleConfig(config, ModuleType.GCD, "gcd", 3.0, 1.0);
        loadModuleConfig(config, ModuleType.SYNC, "sync", 5.0, 1.0);
        loadModuleConfig(config, ModuleType.MACHINE, "machine", 5.0, 1.0);
        loadModuleConfig(config, ModuleType.INTERPOLATION, "interpolation", 4.0, 0.85);
        loadModuleConfig(config, ModuleType.OUTSENS_INTERPOLATION, "interpolation_out", 6.0, 1.0);
        loadModuleConfig(config, ModuleType.OUTSENS_MACHINE, "machine_out", 6.0, 1.0);
    }

    private boolean hasExemptions() {
        return isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING, ExemptType.VEHICLE)
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.getTarget().type != EntityTypes.PLAYER
                || (player.getLastTarget() != null && !player.getTarget().getUuid().equals(player.getLastTarget().getUuid()));
    }

    private void handleCheck(ModuleType type, String alertMessage, Runnable callback) {
        BasicModuleConfig config = configs.get(type);
        if (config == null || !config.enabled) {
            return;
        }

        double currentBuffer = buffers.getOrDefault(type, 0.0) + config.failIncrease;
        buffers.put(type, currentBuffer);

        if (currentBuffer > config.maxBuffer) {
            if (flagAndAlert(alertMessage)) {
                callback.run();
            }
            buffers.put(type, 0.0);
        }
    }

    private void loadModuleConfig(ConfigManager config, ModuleType type, String path, double defaultMaxBuffer, double defaultFailIncrease) {
        if (configs == null) {
            configs = new EnumMap<>(ModuleType.class);
        }
        if (buffers == null) {
            buffers = new EnumMap<>(ModuleType.class);
        }
        String basePath = getConfigName() + "." + path + ".";
        configs.put(type, new BasicModuleConfig(
                config.getBooleanElse(basePath + "enabled", true),
                config.getDoubleElse(basePath + "max-buffer", defaultMaxBuffer),
                config.getDoubleElse(basePath + "fail-increase", defaultFailIncrease)
        ));
        buffers.put(type, 0.0);
    }

    private enum ModuleType {
        GCD,
        SYNC,
        MACHINE,
        INTERPOLATION,
        OUTSENS_MACHINE,
        OUTSENS_INTERPOLATION
    }
}
