package net.minevn.resourcepack;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedInputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.logging.Level;

public class LoadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ResourcePack main = ResourcePack.getInstance();
        main.reloadConfig();
        FileConfiguration config = main.getConfig();
        main.url = config.getString("url");
        sender.sendMessage("§aURL: " + main.url);
        sender.sendMessage("§aGenerating SHA-1 hash...");
        main.hash = sha1FromUrl(main.url);
        sender.sendMessage("§aSHA-1: " + main.hash);
        config.set("hash", main.hash);
        main.saveConfig();
        sender.sendMessage("§aDone.");
        return true;
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
            ResourcePack.getInstance().getLogger().log(Level.SEVERE, "Load SHA1 for file " + link + ": failed", e);
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
}
