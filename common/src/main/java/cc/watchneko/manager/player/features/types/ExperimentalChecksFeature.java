package cc.watchneko.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import cc.watchneko.player.PlayerData;

public class ExperimentalChecksFeature extends GrimFeature {

    public ExperimentalChecksFeature() {
        super("ExperimentalChecks");
    }

    @Override
    public void setState(PlayerData player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExperimentalChecks(true);
            case DISABLED -> player.setExperimentalChecks(false);
            default -> player.setExperimentalChecks(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(PlayerData player) {
        return player.isExperimentalChecks();
    }

    @Override
    public boolean isEnabledInConfig(PlayerData player, ConfigManager config) {
        return config.getBooleanElse("experimental-checks", false);
    }

}
