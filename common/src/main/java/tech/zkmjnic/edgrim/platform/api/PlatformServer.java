package tech.zkmjnic.edgrim.platform.api;

import tech.zkmjnic.edgrim.platform.api.sender.Sender;

public interface PlatformServer {

    String getPlatformImplementationString();

    void dispatchCommand(Sender sender, String command);

    Sender getConsoleSender();

    void registerOutgoingPluginChannel(String name);

    double getTPS();
}
