package tech.zkmjnic.edgrim.platform.api;

public interface PlatformPlugin {
    boolean isEnabled();

    String getName();

    String getVersion();
}
