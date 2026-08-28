package cc.watchneko.platform.bukkit.player;

import cc.watchneko.platform.api.entity.GrimEntity;
import cc.watchneko.platform.api.player.PlatformInventory;
import cc.watchneko.platform.api.player.PlatformPlayer;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.platform.bukkit.WatchNekoBukkitLoaderPlugin;
import cc.watchneko.platform.bukkit.entity.BukkitEntity;
import cc.watchneko.platform.bukkit.utils.anticheat.MultiLibUtil;
import cc.watchneko.platform.bukkit.utils.convert.BukkitConversionUtils;
import cc.watchneko.platform.bukkit.utils.reflection.PaperUtils;
import cc.watchneko.utils.math.Location;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitPlatformPlayer extends BukkitEntity implements PlatformPlayer {
    private static final BukkitAudiences audiences = BukkitAudiences.create(WatchNekoBukkitLoaderPlugin.LOADER);

    @Getter
    private final Player bukkitPlayer;
    private final PlatformInventory inventory;

    public BukkitPlatformPlayer(Player bukkitPlayer) {
        super(bukkitPlayer);
        this.bukkitPlayer = bukkitPlayer;
        this.inventory = new BukkitPlatformInventory(bukkitPlayer);
    }

    @Override
    public void kickPlayer(String textReason) {
        bukkitPlayer.kickPlayer(textReason);
    }

    @Override
    public boolean hasPermission(String s) {
        return bukkitPlayer.hasPermission(s);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return this.bukkitPlayer.hasPermission(new Permission(s, defaultIfUnset ? PermissionDefault.TRUE : PermissionDefault.FALSE));
    }

    @Override
    public boolean isSneaking() {
        return bukkitPlayer.isSneaking();
    }

    @Override
    public void setSneaking(boolean isSneaking) {
        bukkitPlayer.setSneaking(isSneaking);
    }

    @Override
    public void sendMessage(String message) {
        bukkitPlayer.sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
        audiences.player(bukkitPlayer).sendMessage(message);
    }

    @Override
    public boolean isOnline() {
        return bukkitPlayer.isOnline();
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public void updateInventory() {
        bukkitPlayer.updateInventory();
    }

    @Override
    public Vector3d getPosition() {
        org.bukkit.Location location = this.bukkitPlayer.getLocation();
        return new Vector3d(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public PlatformInventory getInventory() {
        return inventory;
    }

    @Override
    public @Nullable GrimEntity getVehicle() {
        return bukkitPlayer.getVehicle() == null ? null : new BukkitEntity(bukkitPlayer.getVehicle());
    }

    @Override
    public GameMode getGameMode() {
        return SpigotConversionUtil.fromBukkitGameMode(bukkitPlayer.getGameMode());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        bukkitPlayer.setGameMode(SpigotConversionUtil.toBukkitGameMode(gameMode));
    }

    public World getBukkitWorld() {
        return bukkitPlayer.getWorld();
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public boolean eject() {
        return bukkitPlayer.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.bukkitPlayer, bLoc);
    }

    @Override
    public boolean isExternalPlayer() {
        return MultiLibUtil.isExternalPlayer(this.bukkitPlayer);
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        this.bukkitPlayer.sendPluginMessage(WatchNekoBukkitLoaderPlugin.LOADER, channelName, byteArray);
    }

    @Override
    public Sender getSender() {
        return WatchNekoBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(this.bukkitPlayer);
    }

    @Override
    @NonNull
    public Player getNative() {
        return this.bukkitPlayer;
    }
}
