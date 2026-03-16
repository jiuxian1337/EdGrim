package tech.zkmjnic.edgrim.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

public class ExperimentalChecksFeature extends GrimFeature {

    public ExperimentalChecksFeature() {
        super("ExperimentalChecks");
    }

    @Override
    public void setState(EdGrimPlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExperimentalChecks(true);
            case DISABLED -> player.setExperimentalChecks(false);
            default -> player.setExperimentalChecks(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(EdGrimPlayer player) {
        return player.isExperimentalChecks();
    }

    @Override
    public boolean isEnabledInConfig(EdGrimPlayer player, ConfigManager config) {
        return config.getBooleanElse("experimental-checks", false);
    }

}
