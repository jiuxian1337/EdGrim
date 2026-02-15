package ac.grim.grimac.platform.bukkit.events;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageMitigationEvent implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        GrimPlayer grimPlayer = findGrimPlayer(player.getUniqueId());
        if (grimPlayer == null || grimPlayer.disableGrim) {
            return;
        }
        if (!grimPlayer.shouldMitigateDamage()) {
            return;
        }
        event.setDamage(0.5);
        grimPlayer.consumeMitigateDamage();
    }

    private GrimPlayer findGrimPlayer(java.util.UUID uuid) {
        for (GrimPlayer entry : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (uuid.equals(entry.getUniqueId())) {
                return entry;
            }
        }
        return null;
    }
}
