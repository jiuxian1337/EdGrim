package tech.zkmjnic.edgrim.command;

import org.incendo.cloud.CommandManager;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;

public interface BuildableCommand {
    void register(CommandManager<Sender> manager);
}
