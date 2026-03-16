package tech.zkmjnic.edgrim.platform.api;

import ac.grim.grimac.api.plugin.GrimPlugin;
import tech.zkmjnic.edgrim.platform.api.manager.ItemResetHandler;
import tech.zkmjnic.edgrim.platform.api.manager.MessagePlaceHolderManager;
import tech.zkmjnic.edgrim.platform.api.manager.CommandAdapter;
import tech.zkmjnic.edgrim.platform.api.manager.PermissionRegistrationManager;
import tech.zkmjnic.edgrim.platform.api.manager.PlatformPluginManager;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayerFactory;
import tech.zkmjnic.edgrim.platform.api.scheduler.PlatformScheduler;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.platform.api.sender.SenderFactory;
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
