package tech.zkmjnic.edgrim.command;

import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;

public interface BuildableCommand {
    void register(CommandManager<Sender> manager);
}
