package tech.zkmjnic.edgrim.platform.bukkit.command;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.platform.api.command.PlayerSelector;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.platform.bukkit.sender.BukkitSenderFactory;

import java.util.Collection;
import java.util.Collections;

public class BukkitPlayerSelectorAdapter implements PlayerSelector {
    private final org.incendo.cloud.bukkit.data.SinglePlayerSelector bukkitSelector;

    public BukkitPlayerSelectorAdapter(org.incendo.cloud.bukkit.data.SinglePlayerSelector bukkitSelector) {
        this.bukkitSelector = bukkitSelector;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Sender getSinglePlayer() {
        return ((BukkitSenderFactory) EdGrimAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single());
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(((BukkitSenderFactory) EdGrimAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single()));
    }

    @Override
    public String inputString() {
        return bukkitSelector.inputString();
    }
}
