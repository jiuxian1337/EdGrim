package tech.zkmjnic.edgrim.command;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.processors.requirements.Requirement;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;

public interface SenderRequirement extends Requirement<Sender, SenderRequirement> {
    @NonNull
    Component errorMessage(Sender sender);
}
