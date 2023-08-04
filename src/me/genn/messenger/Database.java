package me.genn.messenger;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mysql.jdbc.MySQLConnection;
import com.nisovin.magicspells.util.Util;

import me.genn.gennsgym.dbupdates.CardUpdate;
import me.genn.gennsgym.dbupdates.DatabaseUpdate;
import me.genn.gennsgym.dbupdates.ItemToGiveUpdate;
import me.genn.gennsgym.dbupdates.KillUpdate;
import me.genn.gennsgym.dbupdates.SettingUpdate;
import me.genn.gennsgym.dbupdates.StatisticUpdate;
import me.genn.gennsgym.dbupdates.UpgradeUpdate;
import me.genn.messenger.AddFriendResult;
import me.genn.messenger.Messenger;

public class Database {
	private Messenger plugin;
    private MySQLConnection connection;
    private Map<String, Integer> statisticIds = new HashMap();
    public Database(MySQLConnection mySQLConnection) {
    	this.connection = mySQLConnection;
    }
    
    
       
    public Message getPlayerMessage(String playerName, String dateTime, String senderUUID) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if (player != null) {
		    String UUID = player.getUniqueId().toString();
		    if (UUID != null) {
		        try {
		            Message message = getPlayerMessageFromDb(UUID, dateTime, senderUUID);
		            return message;
		        } catch (Exception var11) {
		        }
		    }


		    return null;
		}

        return null;
    }
    
    private Message getPlayerMessageFromDb(String playerUUID, String dateTime, String senderUUID) throws SQLException {
        PreparedStatement stmt = this.connection.prepareStatement("SELECT `message` FROM `message_list` WHERE `receiverUUID` = ?");
        Message message = new Message();
        stmt.setString(1, playerUUID);
        if (stmt.execute()) {
            ResultSet results = stmt.getResultSet();
            if (results != null) {
                while(results.next()) {
                	if (results.getString("message").contains(dateTime) && results.getString("message").contains(senderUUID)) {
                		message = Message.deserialize(results.getString("message"));
                	}
                }
            }

            results.close();
        }

        stmt.close();
        return message;
    }
    
    
        public List<Message> getPlayerMessages(String playerName) {
            Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
			    String UUID = player.getUniqueId().toString();
			    if (UUID != null) {
			        try {
			            List<Message> messages = getPlayerMessagesFromDb(UUID);
			            return messages;
			        } catch (Exception var11) {
			        }
			    }


			    return null;
			}

            return null;
        }
        
        private List<Message> getPlayerMessagesFromDb(String playerUUID) throws SQLException {
            PreparedStatement stmt = this.connection.prepareStatement("SELECT `message` FROM `message_list` WHERE `receiverUUID` = ? ORDER BY `time` DESC");
            List<Message> messages = new ArrayList<Message>();
            stmt.setString(1, playerUUID);
            if (stmt.execute()) {
                ResultSet results = stmt.getResultSet();
                if (results != null) {
                    while(results.next()) {
                    	Message message = Message.deserialize(results.getString("message"));
                    	messages.add(message);
                    }
                }

                results.close();
            }

            stmt.close();
            return messages;
        }
        
        public int getPlayerUnalertedMessages(String playerName) {
            Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
			    String UUID = player.getUniqueId().toString();
			    if (UUID != null) {
			        try {
			            int count = getPlayerUnalertedMessagesFromDb(UUID);
			            return count;
			        } catch (Exception var11) {
			        }
			    }


			    return 0;
			}

            return 0;
        }
        
        private int getPlayerUnalertedMessagesFromDb(String playerUUID) throws SQLException {
            PreparedStatement stmt = this.connection.prepareStatement("SELECT `message` FROM `message_list` WHERE `receiverUUID` = ? AND `alerted` = '0' ORDER BY `time` DESC");
            int counter = 0;
            stmt.setString(1, playerUUID);
            if (stmt.execute()) {
                ResultSet results = stmt.getResultSet();
                if (results != null) {
                    while(results.next()) {
                    	counter++;
                    }
                }

                results.close();
            }

            stmt.close();
            
            stmt = this.connection.prepareStatement("UPDATE `message_list` SET `alerted` = '1' WHERE `receiverUUID` = ? AND `alerted` = '0'");
            stmt.setString(1, playerUUID);
            int count = stmt.executeUpdate();
            
            stmt.close();
            return counter;
        }
        
        public boolean updatePlayerReadMessage(String playerName, Message message) {
            Player player = Bukkit.getPlayerExact(playerName);
            String serialMessage = message.serialize();
            message.read = 1;
            String updatedSerialMessage = message.serialize();
			if (player != null) {
			    String UUID = player.getUniqueId().toString();
			    if (UUID != null) {
			        try {
			            boolean ok = updatePlayerReadMessageToDb(UUID, serialMessage, updatedSerialMessage);
			            return ok;
			        } catch (Exception var11) {
			        }
			    }


			    return false;
			}

            return false;
        }
        
        private boolean updatePlayerReadMessageToDb(String playerUUID, String serialMessage, String updatedSerialMessage) throws SQLException {
        	
            PreparedStatement stmt = this.connection.prepareStatement("UPDATE `message_list` SET `message` = ? WHERE `receiverUUID` = ? AND `message` = ?");
            stmt.setString(1, updatedSerialMessage);
            stmt.setString(2, playerUUID);
            stmt.setString(3, serialMessage);
            int count = stmt.executeUpdate();
            if (count == 0) {
                System.out.println("ERROR: COULD NOT UPDATE ALERTED MESSAGES FOR PLAYER " + playerUUID + ", NO ENTRY IN DATABASE");
                return false;
            }
            stmt.close();
            return true;
        }
        
        public void saveMessage(String playerName, Message message) {
            try {
            	if (Bukkit.getOfflinePlayer(playerName).isOnline()) {
            		String UUID = (Bukkit.getPlayerExact(playerName)).getUniqueId().toString();
                    this.saveMessageIntoDb(UUID, message.serialize(), 1);
            	} else {
            		String UUID = (Bukkit.getOfflinePlayer(playerName)).getUniqueId().toString();
                    this.saveMessageIntoDb(UUID, message.serialize(), 0);
            	}
                
            } catch (SQLException var4) {
                this.plugin.getLogger().severe("SQL ERROR ON SAVING MAILING DATA: " + playerName);
                var4.printStackTrace();
            }

        }
        
        private boolean saveMessageIntoDb(String UUID, String message, int alerted) throws SQLException {
            boolean inDb = false;
            PreparedStatement stmt = this.connection.prepareStatement("INSERT INTO `message_list` (`receiverUUID`, `message`, `time`, `alerted`) VALUES (?, ?, NOW(), ?)");
            stmt.setString(1, UUID);
            stmt.setString(2, message);
            stmt.setInt(3, alerted);
            boolean ok;
            ok = stmt.executeUpdate() > 0;
            stmt.close();
            return ok;
            
        }
        
        
        
        
        public AddFriendResult addFriend(String player, String friend) {
            if (player.equalsIgnoreCase(friend)) {
                return AddFriendResult.INVALID;
            } else {
                PreparedStatement stmt = null;
                ResultSet results = null;

                try {
                    stmt = this.connection.prepareStatement("SELECT `id`, `player1`, `player2`, `mutual` FROM `friends` WHERE (`player1` = ? AND `player2` = ?) OR (`player1` = ? AND `player2` = ?)");
                    stmt.setString(1, player);
                    stmt.setString(2, friend);
                    stmt.setString(3, friend);
                    stmt.setString(4, player);
                    results = stmt.executeQuery();
                    if (results.next()) {
                        int id = results.getInt("id");
                        boolean mutual = results.getInt("mutual") == 1;
                        String player1 = results.getString("player1");
                        results.close();
                        stmt.close();
                        if (mutual) {
                            return AddFriendResult.DUPLICATE_DONE;
                        } else if (player1.equalsIgnoreCase(player)) {
                            return AddFriendResult.DUPLICATE_WAITING;
                        } else {
                            stmt = this.connection.prepareStatement("UPDATE `friends` SET `mutual` = 1 WHERE `id` = ?");
                            stmt.setInt(1, id);
                            if (stmt.executeUpdate() > 0) {
                                stmt.close();
                                return AddFriendResult.ADDED_DONE;
                            } else {
                                stmt.close();
                                return AddFriendResult.INVALID;
                            }
                        }
                    } else {
                        results.close();
                        stmt.close();
                        stmt = this.connection.prepareStatement("INSERT INTO `friends` (`player1`, `player2`) VALUES (?, ?)");
                        stmt.setString(1, player);
                        stmt.setString(2, friend);
                        if (stmt.executeUpdate() > 0) {
                            stmt.close();
                            return AddFriendResult.ADDED_WAITING;
                        } else {
                            stmt.close();
                            return AddFriendResult.INVALID;
                        }
                    }
                } catch (SQLException var9) {
                    var9.printStackTrace();

                    try {
                        if (results != null) {
                            results.close();
                        }

                        if (stmt != null) {
                            stmt.close();
                        }
                    } catch (SQLException var8) {
                        var8.printStackTrace();
                    }

                    return AddFriendResult.INVALID;
                }
            }
        }

        public boolean removeFriend(String player, String friend) {
            PreparedStatement stmt = null;

            try {
                stmt = this.connection.prepareStatement("DELETE FROM `friends` WHERE (`player1` = ? AND `player2` = ?) OR (`player1` = ? AND `player2` = ?) LIMIT 1");
                stmt.setString(1, player);
                stmt.setString(2, friend);
                stmt.setString(3, friend);
                stmt.setString(4, player);
                boolean ok = stmt.executeUpdate() > 0;
                stmt.close();
                return ok;
            } catch (SQLException var7) {
                var7.printStackTrace();

                try {
                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException var6) {
                    var6.printStackTrace();
                }

                return false;
            }
        }

        public String getFriendServer(String player, String friend) {
            PreparedStatement stmt = null;
            ResultSet results = null;

            try {
                stmt = this.connection.prepareStatement("SELECT p.`server_name` FROM `friends` f JOIN `player_server` p ON p.`player_name` = IF(f.`player1` != ?, f.`player1`, f.`player2`) WHERE `mutual` = 1 AND ((`player1` = ? AND `player2` LIKE ?) OR (`player1` LIKE ? AND `player2` = ?)) LIMIT 1");
                stmt.setString(1, player);
                stmt.setString(2, player);
                stmt.setString(3, friend + "%");
                stmt.setString(4, friend + "%");
                stmt.setString(5, player);
                results = stmt.executeQuery();
                String server = null;
                if (results.next()) {
                    server = results.getString("server_name");
                }

                results.close();
                stmt.close();
                return server;
            } catch (SQLException var8) {
                var8.printStackTrace();

                try {
                    if (results != null) {
                        results.close();
                    }

                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException var7) {
                    var7.printStackTrace();
                }

                return null;
            }
        }

        public Map<String, String> getOnlineFriends(String player) {
            Map<String, String> friends = new HashMap();
            PreparedStatement stmt = null;
            ResultSet results = null;

            try {
                stmt = this.connection.prepareStatement("SELECT \tp.`player_name` as `friend_name`, \tp.`server_name`, \tIFNULL(b.`display_name`, '') AS `display_name`, \tIFNULL(g.`name`, '') AS `game_name` FROM `friends` f JOIN `player_server` p ON p.`player_name` = IF(f.`player1` != ?, f.`player1`, f.`player2`) JOIN `bungee_server` b ON b.`server_name` = p.`server_name` LEFT JOIN `mindcrack_status` s ON s.`server_name` = p.`server_name` LEFT JOIN `mindcrack_games` g ON g.`id` = s.`game_id` WHERE f.`mutual` = 1 AND (f.`player1` = ? OR f.`player2` = ?) AND p.`last_update` > DATE_SUB(NOW(), INTERVAL 6 HOUR) ");
                stmt.setString(1, player);
                stmt.setString(2, player);
                stmt.setString(3, player);

                String where;
                for(results = stmt.executeQuery(); results.next(); friends.put(results.getString("friend_name"), where)) {
                    String serverName = results.getString("server_name");
                    where = "";
                    if (serverName.startsWith("GAME_")) {
                        where = "Playing " + results.getString("game_name") + " (" + results.getString("display_name") + ")";
                    } else if (serverName.startsWith("LOBBY_")) {
                        where = "In the lobby";
                    } else {
                        where = "In " + results.getString("game_name") + " (" + results.getString("display_name") + ")";
                    }
                }
            } catch (SQLException var15) {
                var15.printStackTrace();
            } finally {
                try {
                    if (results != null) {
                        results.close();
                    }

                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException var14) {
                    var14.printStackTrace();
                }

            }

            return friends;
        }

        public List<String> getAllFriends(String player) {
            List<String> online = new ArrayList();
            List<String> offline = new ArrayList();
            List<String> requested = new ArrayList();
            PreparedStatement stmt = null;
            ResultSet results = null;

            try {
                stmt = this.connection.prepareStatement("SELECT \tIFNULL(p.`player_name`, IF(f.`player1` != ?, f.`player1`, f.`player2`)) as `friend_name`, \tp.`server_name`,    f.`mutual` FROM `friends` f LEFT JOIN `player_server` p ON p.`player_name` = IF(f.`player1` != ?, f.`player1`, f.`player2`) AND p.`last_update` > DATE_SUB(NOW(), INTERVAL 6 HOUR) WHERE (f.`player1` = ? OR (f.`player2` = ? AND f.`mutual`= 1)) ORDER BY `friend_name`");
                stmt.setString(1, player);
                stmt.setString(2, player);
                stmt.setString(3, player);
                stmt.setString(4, player);
                results = stmt.executeQuery();

                label129:
                while(true) {
                    while(true) {
                        if (!results.next()) {
                            break label129;
                        }

                        String friendName = results.getString("friend_name");
                        String serverName = results.getString("server_name");
                        boolean mutual = results.getInt("mutual") == 1;
                        if (!mutual) {
                            requested.add(ChatColor.GRAY + friendName);
                        } else if (serverName != null && !serverName.isEmpty()) {
                            online.add(ChatColor.GREEN + friendName);
                        } else {
                            offline.add(ChatColor.RED + friendName);
                        }
                    }
                }
            } catch (SQLException var18) {
                var18.printStackTrace();
            } finally {
                try {
                    if (results != null) {
                        results.close();
                    }

                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException var17) {
                    var17.printStackTrace();
                }

            }

            List<String> all = new ArrayList();
            all.addAll(online);
            all.addAll(offline);
            all.addAll(requested);
            return all;
        }

}
