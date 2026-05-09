package tech.zkmjnic.edgrim.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tech.zkmjnic.edgrim.player.PlayerData;

@RequiredArgsConstructor
@Getter
public abstract class GrimFeature {

    private final String name;

    public abstract void setState(PlayerData player, ConfigManager config, FeatureState state);

    public abstract boolean isEnabled(PlayerData player);

    public abstract boolean isEnabledInConfig(PlayerData player, ConfigManager config);

}
