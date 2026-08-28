package cc.watchneko.platform.api;

import ac.grim.grimac.api.plugin.GrimPlugin;
import cc.watchneko.platform.api.manager.*;
import cc.watchneko.platform.api.player.PlatformPlayerFactory;
import cc.watchneko.platform.api.scheduler.PlatformScheduler;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.platform.api.sender.SenderFactory;
import com.github.retrooper.packetevents.PacketEventsAPI;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;

public interface PlatformLoader {
    PlatformScheduler getScheduler();

    PlatformPlayerFactory getPlatformPlayerFactory();

    CommandAdapter getCommandAdapter();

    PacketEventsAPI<?> getPacketEvents();

    CommandManager<Sender> getCommandManager();

    ItemResetHandler getItemResetHandler();

    SenderFactory<?> getSenderFactory();

    GrimPlugin getPlugin();

    PlatformPluginManager getPluginManager();

    PlatformServer getPlatformServer();

    // Intended for use for platform specific service/API bringup
    // Method will be called when InitManager.load() is called
    void registerAPIService();

    // Used to replace text placeholders in messages
    // Currently only supports PlaceHolderAPI on Bukkit
    @NonNull
    MessagePlaceHolderManager getMessagePlaceHolderManager();

    PermissionRegistrationManager getPermissionManager();
}
