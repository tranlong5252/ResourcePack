package net.minevn.resourcepack;

import com.destroystokyo.paper.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ResourcePack extends JavaPlugin implements Listener {

    String url, hash;
    Location loc;
    private final Map<UUID, Integer> tries = new HashMap<>();

    @Override
    public void onEnable() {
        _instance = this;
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        url = config.getString("url");
        hash = config.getString("hash");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("loadrsp").setExecutor(new LoadCommand());
        getCommand("setrsploc").setExecutor(new SetLocCommand());
        String locStr = config.getString("loc");
        if (locStr != null) {
            loc = getDeserializedLocation(locStr);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().setResourcePack(url, hash);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        tries.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcePack(final PlayerResourcePackStatusEvent e) {
        Player player = e.getPlayer();
        PlayerResourcePackStatusEvent.Status s = e.getStatus();
        Bukkit.getScheduler().runTask(this, () -> {
            switch (s) {
                case DECLINED: {
                    if (loc != null) {
                        player.teleport(loc);
                        break;
                    }
                    player.kickPlayer(
                            "§c§lBạn không thể chơi vì đã chọn không cài gói tài nguyên, " +
                                    "xem hướng dẫn cài lại tại link sau:"
                                    + "\n§ahttp://minefs.net/blog/index.php/2016/09/23/" +
                                    "huong-dan-cai-dat-goi-tai-nguyen/");
                    player.sendTitle(new Title("", "", 0, 1, 0));
                    break;
                }
                case ACCEPTED: {
                    break;
                }
                case FAILED_DOWNLOAD: {
                    int tried = tries.getOrDefault(player.getUniqueId(), 0);
                    if (tried < 5) {
                        player.sendMessage("§cĐang thử cài lại gói tài nguyên ("+ (++tried) +")...");
                        tries.put(player.getUniqueId(), tried);
                        e.getPlayer().setResourcePack(url, hash);
                    } else {
                        player.kickPlayer("§c§lTải gói tài nguyên thất bại, hãy thử vào lại."
                                + "\n§e§lNếu bị nhiều lần hãy thử xóa thư mục §b§lserver-resource-packs " +
                                "§e§ltrong §b§l.minecraft §e§lvà vào lại server.");
                    }
                    break;
                }
                case SUCCESSFULLY_LOADED: {
                    player.sendTitle("§a§lCài đặt thành công.", "", 0, 60, 10);
                    player.sendMessage("§a§lĐã cài đặt thành công gói tài nguyên.");
                    tries.remove(player.getUniqueId());
                    checkRspLoc(player);
                    break;
                }
            }
        });
    }

    public String getSerializedLocation(final Location location) {
        return location.getWorld().getName() + "," + (location.getBlockX() + 0.5) + ","
                + location.getBlockY() + "," + (location.getBlockZ() + 0.5) + "," + location.getYaw() + ","
                + location.getPitch();
    }

    public Location getDeserializedLocation(final String s) {
        if (s == null) {
            return null;
        }
        final String[] split = s.split(",");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2])
                + 1.0, Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
    }

    public void checkRspLoc(Player p) {
        if (loc == null || p.getWorld() != loc.getWorld() || p.getLocation().distance(loc) > 10) {
            return;
        }
        try {
            p.setOp(true);
            Bukkit.dispatchCommand(p, "spawn");
        } finally {
            p.setOp(false);
        }
    }

    // singleton
    private static ResourcePack _instance;

    public static ResourcePack getInstance() {
        return _instance;
    }
}
