package tech.zkmjnic.edgrim.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import tech.zkmjnic.edgrim.player.PlayerData;

public class ForceSlowMovementFeature extends GrimFeature {

    public ForceSlowMovementFeature() {
        super("ForceSlowMovement");
    }

    @Override
    public void setState(PlayerData player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setForceSlowMovement(true);
            case DISABLED -> player.setForceSlowMovement(false);
            default -> player.setForceSlowMovement(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(PlayerData player) {
        return player.isForceSlowMovement();
    }

    @Override
    public boolean isEnabledInConfig(PlayerData player, ConfigManager config) {
        return config.getBooleanElse("force-slow-movement", true);
    }

}
