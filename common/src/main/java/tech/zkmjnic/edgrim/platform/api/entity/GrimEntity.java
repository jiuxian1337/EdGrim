package tech.zkmjnic.edgrim.platform.api.entity;

import ac.grim.grimac.api.GrimIdentity;
import org.checkerframework.checker.nullness.qual.NonNull;
import tech.zkmjnic.edgrim.platform.api.world.PlatformWorld;
import tech.zkmjnic.edgrim.utils.math.Location;

import java.util.concurrent.CompletableFuture;

public interface GrimEntity extends GrimIdentity {
    /**
     * Eject any passenger.
     *
     * @return True if there was a passenger.
     */
    boolean eject();

    CompletableFuture<Boolean> teleportAsync(Location location);

    @NonNull
    Object getNative();

    boolean isDead();

    PlatformWorld getWorld();

    Location getLocation();
}
