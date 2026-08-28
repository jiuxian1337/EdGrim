package cc.watchneko.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import cc.watchneko.player.PlayerData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class GrimFeature {

    private final String name;

    public abstract void setState(PlayerData player, ConfigManager config, FeatureState state);

    public abstract boolean isEnabled(PlayerData player);

    public abstract boolean isEnabledInConfig(PlayerData player, ConfigManager config);

}
