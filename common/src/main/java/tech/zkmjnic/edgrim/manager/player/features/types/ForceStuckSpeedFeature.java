package tech.zkmjnic.edgrim.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import tech.zkmjnic.edgrim.player.PlayerData;

public class ForceStuckSpeedFeature extends GrimFeature {

    public ForceStuckSpeedFeature() {
        super("ForceStuckSpeed");
    }

    @Override
    public void setState(PlayerData player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setForceStuckSpeed(true);
            case DISABLED -> player.setForceStuckSpeed(false);
            default -> player.setForceStuckSpeed(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(PlayerData player) {
        return player.isForceStuckSpeed();
    }

    @Override
    public boolean isEnabledInConfig(PlayerData player, ConfigManager config) {
        return config.getBooleanElse("force-stuck-speed", true);
    }

}
