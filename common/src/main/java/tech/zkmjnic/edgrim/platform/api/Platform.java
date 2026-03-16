package tech.zkmjnic.edgrim.platform.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@AllArgsConstructor
public enum Platform {

    BUKKIT("bukkit"),
    FOLIA("folia");

    @Getter private final String name;

    public static @Nullable Platform getByName(String name) {
        for (Platform platform : values()) {
            if (platform.getName().equalsIgnoreCase(name)) return platform;
        }
        return null;
    }

}
