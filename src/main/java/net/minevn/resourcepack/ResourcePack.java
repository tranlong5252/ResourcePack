package net.minevn.resourcepack;

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
    Location firstJoinLoc;
    FileConfiguration config = getConfig();
    private final Map<UUID, Integer> tries = new HashMap<>();

    @Override
    public void onEnable() {
        _instance = this;
        saveDefaultConfig();
        url = config.getString("url");
        hash = config.getString("hash");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("loadrsp").setExecutor(new LoadCommand());
        getCommand("setrsploc").setExecutor(new SetLocCommand());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.sendTitle("§aXin chờ","§eĐang cài gói tài nguyên...",0,600,0);
        Bukkit.getScheduler().runTaskLater(_instance,() -> p.setResourcePack(url, hash),60);
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
                    String locStr = config.getString("loc");
                    if (locStr != null) {
                        loc = getDeserializedLocation(locStr);
                    }
                    if (loc != null) {
                        try {
                            player.teleport(loc);
                            getLogger().warning(player.getName() +  " Đã từ chối cài rsp!");
                            player.resetTitle();
                        } catch (IllegalArgumentException ex) { getLogger().warning("Tọa độ không xác định! " + loc); }
                    } else {
                       player.kickPlayer("§c§lBạn không thể chơi vì đã chọn không cài gói tài " +
                               "nguyên, xem hướng dẫn cài lại tại link sau:" + "\n§a" +
                               "www.minevn.net/blog/huong-dan/huong-dan-cai-dat-goi-tai-nguyen.html");
                    }
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
            if (p.hasPermission("resourcepack.firstjoin")) {
                String locStr = config.getString("firstJoinLoc");
                if (locStr != null) {
                    firstJoinLoc = getDeserializedLocation(locStr);
                    try {
                        p.teleport(firstJoinLoc);
                    } catch (IllegalArgumentException ex) {
                        getLogger().warning("Tọa độ không xác định! " + firstJoinLoc);
                    }
                }
            }
            else Bukkit.dispatchCommand(p, "spawn");
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
