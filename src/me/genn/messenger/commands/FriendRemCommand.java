//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.genn.messenger.commands;

import me.genn.messenger.Messenger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class FriendRemCommand implements CommandExecutor, TabCompleter {
	Messenger plugin;

    public FriendRemCommand(Messenger plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String alias, String[] args) {
        if (sender instanceof Player) {
            if (args.length != 1) {
                sender.sendMessage(this.plugin.chatColorMain + "Usage: " + this.plugin.chatColorHighlight + "/" + alias + " " + ChatColor.ITALIC + "name");
                return true;
            }

            final String name = args[0];
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                public void run() {
                    boolean removed = FriendRemCommand.this.plugin.database.removeFriend(sender.getName(), name);
                    FriendRemCommand.this.handleResult(sender, name, removed);
                }
            });
        }

        return true;
    }

    void handleResult(final CommandSender sender, final String name, final boolean removed) {
        Bukkit.getScheduler().runTask(this.plugin, new Runnable() {
            public void run() {
                if (removed) {
                    sender.sendMessage(FriendRemCommand.this.plugin.chatColorMain + "You and " + FriendRemCommand.this.plugin.chatColorHighlight + name + FriendRemCommand.this.plugin.chatColorMain + " are no longer friends.");
                } else {
                    sender.sendMessage(FriendRemCommand.this.plugin.chatColorMain + "Unable to remove friend (that person is probably not your friend already).");
                }

            }
        });
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList();
        List<String> names = (List)this.plugin.allFriends.get(sender.getName());
        if (args.length <= 1 && names != null && names.size() > 0) {
            Iterator var8 = names.iterator();

            while(true) {
                String name;
                do {
                    if (!var8.hasNext()) {
                        return list;
                    }

                    name = (String)var8.next();
                } while(args.length != 0 && !name.toLowerCase().startsWith(args[0].toLowerCase()));

                list.add(name);
            }
        } else {
            return list;
        }
    }
}
