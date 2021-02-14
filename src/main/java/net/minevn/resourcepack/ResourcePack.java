package net.minevn.resourcepack;

import com.destroystokyo.paper.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ResourcePack extends JavaPlugin implements Listener {

    private String url, hash;
    private final Map<UUID, Integer> tries = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        url = config.getString("url");
        hash = config.getString("hash");
        getServer().getPluginManager().registerEvents(this, this);
    }

    public String sha1FromUrl(String link) {
        try {
            byte[] buffer = new byte[8192];
            int count;
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            URL url = new URL(link);
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            while ((count = bis.read(buffer)) > 0)
                digest.update(buffer, 0, count);
            bis.close();
            byte[] hash = digest.digest();
            return byteArrayToHexString(hash);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Load SHA1 for file " + link + ": failed", e);
            return null;
        }
    }

    public String byteArrayToHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
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
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                switch (s) {
                    case DECLINED: {
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
                        break;
                    }
                }
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        FileConfiguration config = getConfig();
        url = config.getString("url");
        sender.sendMessage("§aURL: " + url);
        sender.sendMessage("§aGenerating SHA-1 hash...");
        hash = sha1FromUrl(url);
        sender.sendMessage("§aSHA-1: " + hash);
        config.set("hash", hash);
        saveConfig();
        sender.sendMessage("§aDone.");
        return true;
    }
}
