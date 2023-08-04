//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.genn.messenger.commands;

import me.genn.messenger.Messenger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FriendListCommand implements CommandExecutor {
	Messenger plugin;

    public FriendListCommand(Messenger plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String alias, String[] args) {
        if (sender instanceof Player) {
            if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                    public void run() {
                        List<String> friends = FriendListCommand.this.plugin.database.getAllFriends(sender.getName());
                        FriendListCommand.this.listAllFriends(sender, friends);
                    }
                });
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                    public void run() {
                        Map<String, String> friends = FriendListCommand.this.plugin.database.getOnlineFriends(sender.getName());
                        FriendListCommand.this.listOnlineFriends(sender, friends);
                    }
                });
            }
        }

        return true;
    }

    void listAllFriends(final CommandSender sender, final List<String> friends) {
        Bukkit.getScheduler().runTask(this.plugin, new Runnable() {
            public void run() {
                List<String> names = new ArrayList();
                sender.sendMessage(FriendListCommand.this.plugin.chatColorMain + "You have " + FriendListCommand.this.plugin.chatColorHighlight + friends.size() + FriendListCommand.this.plugin.chatColorMain + " friends" + ChatColor.WHITE + (friends.size() == 0 ? "." : ":"));
                if (friends.size() > 0) {
                    String msg = "";
                    Iterator var4 = friends.iterator();

                    while(var4.hasNext()) {
                        String name = (String)var4.next();
                        if (ChatColor.stripColor(msg).length() + ChatColor.stripColor(name).length() > 55) {
                            sender.sendMessage(msg);
                            msg = "";
                        }

                        if (msg.isEmpty()) {
                            msg = "  ";
                        } else {
                            msg = msg + ChatColor.WHITE + ", ";
                        }

                        msg = msg + name;
                        names.add(ChatColor.stripColor(name));
                    }

                    sender.sendMessage(msg);
                }

                FriendListCommand.this.plugin.allFriends.put(sender.getName(), names);
            }
        });
    }

    void listOnlineFriends(final CommandSender sender, final Map<String, String> friends) {
        Bukkit.getScheduler().runTask(this.plugin, new Runnable() {
            public void run() {
                List<String> names = new ArrayList();
                if (friends.size() > 0) {
                    sender.sendMessage(FriendListCommand.this.plugin.chatColorMain + "You have " + FriendListCommand.this.plugin.chatColorHighlight + friends.size() + FriendListCommand.this.plugin.chatColorMain + " friend" + (friends.size() != 1 ? "s" : "") + " online" + ChatColor.WHITE + ":");
                    Iterator var3 = friends.keySet().iterator();

                    while(var3.hasNext()) {
                        String name = (String)var3.next();
                        sender.sendMessage("  " + FriendListCommand.this.plugin.chatColorHighlight + name + ChatColor.WHITE + " - " + FriendListCommand.this.plugin.chatColorMain + (String)friends.get(name));
                        names.add(name);
                    }
                } else {
                    sender.sendMessage(FriendListCommand.this.plugin.chatColorMain + "You have " + FriendListCommand.this.plugin.chatColorHighlight + "0" + FriendListCommand.this.plugin.chatColorMain + " friends online.");
                }

                FriendListCommand.this.plugin.onlineFriends.put(sender.getName(), names);
            }
        });
    }
}
