package cc.watchneko.platform.bukkit.command;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.platform.api.command.PlayerSelector;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.platform.bukkit.sender.BukkitSenderFactory;

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
        return ((BukkitSenderFactory) WatchNekoAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single());
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(((BukkitSenderFactory) WatchNekoAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single()));
    }

    @Override
    public String inputString() {
        return bukkitSelector.inputString();
    }
}
