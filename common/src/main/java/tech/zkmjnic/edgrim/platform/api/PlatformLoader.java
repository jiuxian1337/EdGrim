package tech.zkmjnic.edgrim.platform.api;

import ac.grim.grimac.api.plugin.GrimPlugin;
import com.github.retrooper.packetevents.PacketEventsAPI;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import tech.zkmjnic.edgrim.platform.api.manager.*;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayerFactory;
import tech.zkmjnic.edgrim.platform.api.scheduler.PlatformScheduler;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.platform.api.sender.SenderFactory;

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
