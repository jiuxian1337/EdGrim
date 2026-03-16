package tech.zkmjnic.edgrim.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class GrimFeature {

    private final String name;

    public abstract void setState(EdGrimPlayer player, ConfigManager config, FeatureState state);

    public abstract boolean isEnabled(EdGrimPlayer player);

    public abstract boolean isEnabledInConfig(EdGrimPlayer player, ConfigManager config);

}
