package tech.zkmjnic.edgrim.platform.bukkit.entity;

import tech.zkmjnic.edgrim.platform.api.entity.GrimEntity;
import tech.zkmjnic.edgrim.platform.api.world.PlatformWorld;
import tech.zkmjnic.edgrim.platform.bukkit.utils.convert.BukkitConversionUtils;
import tech.zkmjnic.edgrim.platform.bukkit.utils.reflection.PaperUtils;
import tech.zkmjnic.edgrim.platform.bukkit.world.BukkitPlatformWorld;
import tech.zkmjnic.edgrim.utils.math.Location;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitEdGrimEntity implements GrimEntity {

    private final Entity entity;
    private BukkitPlatformWorld bukkitPlatformWorld;

    public BukkitEdGrimEntity(Entity entity) {
        Objects.requireNonNull(entity);
        this.entity = entity;
    }

    public Entity getBukkitEntity() {
        return this.entity;
    }

    @Override
    public UUID getUniqueId() {
        return entity.getUniqueId();
    }

    @Override
    public boolean eject() {
        return entity.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.entity, bLoc);
    }

    @Override
    @NonNull
    public Entity getNative() {
        return entity;
    }

    @Override
    public boolean isDead() {
        return this.entity.isDead();
    }

    // TODO replace with PlayerWorldChangeEvent listener instead of checking for equality for better performance
    @Override
    public PlatformWorld getWorld() {
        if (bukkitPlatformWorld == null || !bukkitPlatformWorld.getBukkitWorld().equals(entity.getWorld())) {
            bukkitPlatformWorld = new BukkitPlatformWorld(entity.getWorld());
        }

        return bukkitPlatformWorld;
    }

    @Override
    public Location getLocation() {
        org.bukkit.Location location = this.entity.getLocation();
        return new Location(
                this.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }
}
