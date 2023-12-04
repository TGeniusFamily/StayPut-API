package nl.zandervdm.stayput.Listeners;

import com.earth2me.essentials.*;
import com.earth2me.essentials.commands.NoChargeException;
import com.onarandombox.MultiverseCore.event.MVTeleportEvent;

import net.ess3.api.events.UserRandomTeleportEvent;
import nl.zandervdm.stayput.Database.PlayerLocation;
import nl.zandervdm.stayput.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PlayerTeleportEventListener implements Listener {
    protected Main plugin;
    protected List<String> playersInTeleportQueue;
    public PlayerTeleportEventListener(Main plugin) {
        this.plugin = plugin;
        playersInTeleportQueue = new ArrayList<>();
    }


    @EventHandler
    public void onPlayerMVTeleportEvent(MVTeleportEvent event) {
        Location location = event.getDestination().getLocation(event.getTeleportee());

        if (location == null) {
            this.plugin.debugLogger("onPlayerMVTeleportEvent " + event.getDestination().getType()
                    + " Destination location is null apparently?");
        } else {
            this.plugin.debugLogger("onPlayerMVTeleportEvent " + event.getDestination().getType() + " "
                    + location);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        playersInTeleportQueue.remove(event.getPlayer().getName());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) throws InterruptedException, ExecutionException {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause.equals(PlayerTeleportEvent.TeleportCause.COMMAND) ||
                cause.equals(PlayerTeleportEvent.TeleportCause.PLUGIN))
        {
            this.plugin.debugLogger("onPlayerTeleportEvent " + cause.toString());
            Location newLocation = this.plugin.getTeleport().handleTeleport(event.getPlayer(), event.getFrom(), event.getTo());
            if (newLocation != null) {
                event.setTo(newLocation);
            } else {
                if (!this.plugin.getRuleManager().shouldTeleportPlayerNoLocationExistsCheck(event.getPlayer(), event.getFrom(), event.getTo())){
                    return;
                }

                if (playersInTeleportQueue.contains(event.getPlayer().getName())) {
                    event.getPlayer().sendMessage(this.plugin.getConfig().getString("mod.messages.please_wait"));
                    return;
                }
                if (!this.plugin.getConfig().getStringList("mod.worlds_with_rtp").contains(event.getTo().getWorld().getName())) {
                    return;
                }
                Essentials plugin = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
                User user = plugin.getUser(event.getPlayer());
                if (user.hasHome("home")) {
                    event.setTo(user.getHome("home"));
                } else {
                    event.setCancelled(true);
                    playersInTeleportQueue.add(event.getPlayer().getName());
                    RandomTeleport randomTeleport = plugin.getRandomTeleport();
//                    UserRandomTeleportEvent ev = new UserRandomTeleportEvent(user, randomTeleport.getCenter(), randomTeleport.getMinRange(), randomTeleport.getMaxRange());
                    int minRange = this.plugin.getConfig().getInt("mod.min_range");
                    int maxRange = this.plugin.getConfig().getInt("mod.max_range");

                    randomTeleport.getRandomLocation(new Location(plugin.getWorld(event.getTo().getWorld().getName()), 0, 0, 0), minRange,  maxRange).thenAccept((loc) -> {
                        CompletableFuture<Boolean> future = new CompletableFuture<>();
                        Trade charge = new Trade(event.getPlayer().getName(), plugin);
                        user.getAsyncTeleport().teleport(loc, charge, PlayerTeleportEvent.TeleportCause.PLUGIN, future);
                        future.thenAccept((isTeleported) -> {
                            event.getPlayer().sendMessage(this.plugin.getConfig().getString("mod.messages.you_have_been_randomly_teleported"));
                            playersInTeleportQueue.remove(event.getPlayer().getName());

                        });
                    });

                }


            }
        }


    }

}
