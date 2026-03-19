package tech.zkmjnic.edgrim.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import tech.zkmjnic.edgrim.player.PlayerData;

public class ExemptElytraFeature extends GrimFeature {

    public ExemptElytraFeature() {
        super("ExemptElytra");
    }

    @Override
    public void setState(PlayerData player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExemptElytra(true);
            case DISABLED -> player.setExemptElytra(false);
            default -> player.setExemptElytra(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(PlayerData player) {
        return player.isExemptElytra();
    }

    @Override
    public boolean isEnabledInConfig(PlayerData player, ConfigManager config) {
        return config.getBooleanElse("exempt-elytra", false);
    }

}
