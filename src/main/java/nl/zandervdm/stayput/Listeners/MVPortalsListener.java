package nl.zandervdm.stayput.Listeners;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.RandomTeleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.onarandombox.MultiversePortals.event.MVPortalEvent;
import nl.zandervdm.stayput.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MVPortalsListener implements Listener {
    protected Main plugin;

    public MVPortalsListener(Main plugin) {
        this.plugin = plugin;
        playersInTeleportQueue = new ArrayList<>();
    }

    protected List<String> playersInTeleportQueue;


    @EventHandler
    public void OnTeleport(PlayerTeleportEvent event) {
        Player teleportee = event.getPlayer();
        if (event.getFrom() == event.getTo()) return;
        this.plugin.debugLogger("OnTeleport");

        this.plugin.getTeleport().handleTeleport(teleportee, event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onMVPortalEvent(MVPortalEvent event) {
        this.plugin.debugLogger("onMVPortalEvent");
        Player teleportee = event.getTeleportee();
        Location newLocation = this.plugin.getTeleport().handleTeleport(teleportee, event.getFrom(), event.getDestination().getLocation(teleportee));
        Location getTo = event.getDestination().getLocation(teleportee);
        if (newLocation != null) {
            event.setCancelled(true);
            plugin.debugLogger("teleporting to newLocation....");
            teleportee.teleport(newLocation);
            return;
        }
        if (!this.plugin.getRuleManager().shouldTeleportPlayerNoLocationExistsCheck(teleportee, event.getFrom(), getTo)) {
            return;
        }

        if (playersInTeleportQueue.contains(teleportee.getName())) {
            teleportee.sendMessage(this.plugin.getConfig().getString("mod.messages.please_wait"));
            return;
        }
        if (!this.plugin.getConfig().getStringList("mod.worlds_with_rtp").contains(getTo.getWorld().getName())) {
            return;
        }
        Essentials pluginEssentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        User user = pluginEssentials.getUser(teleportee);
        event.setCancelled(true);

        if (user.hasHome("home")) {
            this.plugin.debugLogger("teleporting to home home....");
            teleportee.teleport(user.getHome("home"));
            return;
        }
        if (user.hasHome()) {
            this.plugin.debugLogger("teleporting to home (other)....");
            teleportee.teleport(user.getHome(user.getHomes().get(0)));
            return;
        }
        playersInTeleportQueue.add(teleportee.getName());
        RandomTeleport randomTeleport = pluginEssentials.getRandomTeleport();
        int minRange = this.plugin.getConfig().getInt("mod.min_range");
        int maxRange = this.plugin.getConfig().getInt("mod.max_range");
        this.plugin.debugLogger("teleporting to rtp....");

        randomTeleport.getRandomLocation(new Location(pluginEssentials.getWorld(getTo.getWorld().getName()), 0, 0, 0), minRange, maxRange).thenAccept((loc) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                teleportee.teleport(loc);
                teleportee.sendMessage(this.plugin.getConfig().getString("mod.messages.you_have_been_randomly_teleported"));
                playersInTeleportQueue.remove(teleportee.getName());
                //Bukkit.broadcastMessage is not thread-safe
            });

        });


    }

}
