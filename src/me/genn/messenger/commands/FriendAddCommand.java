//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.genn.messenger.commands;

import me.genn.messenger.AddFriendResult;
import me.genn.messenger.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FriendAddCommand implements CommandExecutor {
    Messenger plugin;

    public FriendAddCommand(Messenger plugin) {
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
                    AddFriendResult result = FriendAddCommand.this.plugin.database.addFriend(sender.getName(), name);
                    FriendAddCommand.this.handleResult(sender, name, result);
                }
            });
        }

        return true;
    }

    void handleResult(final CommandSender sender, final String name, final AddFriendResult result) {
        Bukkit.getScheduler().runTask(this.plugin, new Runnable() {
            public void run() {
                if (result == AddFriendResult.ADDED_WAITING) {
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "Friend request successful!");
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "Now have " + FriendAddCommand.this.plugin.chatColorHighlight + name + FriendAddCommand.this.plugin.chatColorMain + " add you as a friend.");
                } else if (result == AddFriendResult.ADDED_DONE) {
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "Friend request successful!");
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "You and " + FriendAddCommand.this.plugin.chatColorHighlight + name + FriendAddCommand.this.plugin.chatColorMain + " are now friends.");
                    Player friend = Bukkit.getPlayerExact(name);
                    if (friend != null) {
                        friend.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "You and " + FriendAddCommand.this.plugin.chatColorHighlight + sender.getName() + FriendAddCommand.this.plugin.chatColorMain + " are now friends.");
                    }
                } else if (result == AddFriendResult.DUPLICATE_DONE) {
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "You and " + FriendAddCommand.this.plugin.chatColorHighlight + name + FriendAddCommand.this.plugin.chatColorMain + " are already friends.");
                } else if (result == AddFriendResult.DUPLICATE_WAITING) {
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "You have already sent that friend request.");
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "You are waiting on " + FriendAddCommand.this.plugin.chatColorHighlight + name + FriendAddCommand.this.plugin.chatColorMain + " to add you as a friend.");
                } else if (result == AddFriendResult.INVALID) {
                    sender.sendMessage(FriendAddCommand.this.plugin.chatColorMain + "That is not a valid friend request.");
                }

            }
        });
    }
}
