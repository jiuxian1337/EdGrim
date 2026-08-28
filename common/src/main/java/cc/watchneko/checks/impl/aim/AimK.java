package cc.watchneko.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.lists.EvictingList;
import cc.watchneko.utils.math.MathUtil;

import java.util.List;

@CheckData(
        name = "AimK",
        description = "abnormally smooth micro-rotation windows"
)
public final class AimK extends Check implements RotationCheck {
    private final List<Float> yawSamples = new EvictingList<>(128);
    private final List<Float> pitchSamples = new EvictingList<>(128);
    private double minimumLevel = 90.0;

    public AimK(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        final float deltaYaw = Math.abs(MathUtil.getAngleDifference(update.getTo().getYaw(), update.getFrom().getYaw()));
        final float deltaPitch = Math.abs(MathUtil.getAngleDifference(update.getTo().getPitch(), update.getFrom().getPitch()));

        if (deltaYaw > 0.0 && deltaPitch > 0.0 && deltaYaw < 20.f && deltaPitch < 20.f && !update.isCinematic()) {
            yawSamples.add(deltaYaw);
            pitchSamples.add(deltaPitch);
        }

        if (yawSamples.size() < 128 || pitchSamples.size() < 128) {
            return;
        }

        int level = 0;
        for (Float delta : yawSamples) {
            if (delta != 0.0F && delta < 0.001F) level++;
        }
        for (Float delta : pitchSamples) {
            if (delta != 0.0F && delta < 0.001F) level++;
        }

        final double averageYaw = yawSamples.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        final double averagePitch = pitchSamples.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        final boolean invalid = (averageYaw < 1.1 && averageYaw > 0.0) || averagePitch <= 0.01;

        if (invalid && level >= minimumLevel && flagAndAlert("y=" + averageYaw + " p=" + averagePitch + " l=" + level)) {
            player.mitigateDamage();
        }

        yawSamples.clear();
        pitchSamples.clear();
    }

    @Override
    public void onReload(ConfigManager config) {
        minimumLevel = config.getDoubleElse("AimK.minimum-level-threshold", minimumLevel);
    }
}
