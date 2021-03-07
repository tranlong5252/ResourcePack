package net.minevn.resourcepack;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLocCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Vao server roi dung lenh nay");
            return true;
        }
        Player p = ((Player) sender);
        ResourcePack main = ResourcePack.getInstance();
        main.loc = p.getLocation();
        main.getConfig().set("loc", main.getSerializedLocation(main.loc));
        main.saveConfig();
        p.sendMessage("done.");
        return true;
    }
}
