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
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mysql.jdbc.MySQLConnection;
import com.nisovin.magicspells.util.Util;


import me.genn.messenger.Messenger;

public class Database {
	private Messenger plugin;
    private MySQLConnection connection;
    private Map<String, Integer> statisticIds = new HashMap();
    public Database(Messenger plugin) {
        this.plugin = plugin;
    }
    public boolean connect(String host, String user, String pass, String db) {
        this.connection = this.openConnection(host, user, pass, db);
        return this.connection != null;
    }

    public MySQLConnection getConnection() {
        return this.connection;
    }
    
    private MySQLConnection openConnection(String host, String user, String pass, String db) {
        try {
            return (MySQLConnection)DriverManager.getConnection("jdbc:mysql://" + host + "/" + db, user, pass);
        } catch (SQLException var6) {
            var6.printStackTrace();
            return null;
        }
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
            PreparedStatement stmt = this.connection.prepareStatement("SELECT `message` FROM `message_list` WHERE `receiverUUID` = ?");
            List<Message> messages = new ArrayList();
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
        
        public void saveMessage(String playerName, Message message) {
            try {
            	if (Bukkit.getOfflinePlayer(playerName).isOnline()) {
            		String UUID = (Bukkit.getPlayerExact(playerName)).getUniqueId().toString();
                    this.saveMessageIntoDb(UUID, message.serialize());
            	} else {
            		String UUID = (Bukkit.getOfflinePlayer(playerName)).getUniqueId().toString();
                    this.saveMessageIntoDb(UUID, message.serialize());
            	}
                
            } catch (SQLException var4) {
                this.plugin.getLogger().severe("SQL ERROR ON SAVING MAILING DATA: " + playerName);
                var4.printStackTrace();
            }

        }
        
        private boolean saveMessageIntoDb(String UUID, String message) throws SQLException {
            boolean inDb = false;
            PreparedStatement stmt = this.connection.prepareStatement("INSERT INTO `message_list` (`receiverUUID`, `message`) VALUES (?, ?)");
            stmt.setString(1, UUID);
            stmt.setString(2, message);
            boolean ok;
            ok = stmt.executeUpdate() > 0;
            stmt.close();
            return ok;
            
        }
        
        
        public void saveMailingData(String playerName, UUID ID, MailingData mailingdata) {
            try {
                String UUID = ID.toString();
                this.saveMailingDataIntoDb(playerName, UUID, mailingdata.serialize());
            } catch (SQLException var4) {
                this.plugin.getLogger().severe("SQL ERROR ON SAVING MAILING DATA: " + playerName);
                var4.printStackTrace();
            }

        }
        private boolean saveMailingDataIntoDb(String playerName, String UUID, String mailingdata) throws SQLException {
            boolean inDb = false;
            PreparedStatement stmt = this.connection.prepareStatement("SELECT `id` FROM `mailing_profile_data` WHERE `player_name` = ?");
            stmt.setString(1, playerName);
            if (stmt.execute()) {
                ResultSet results = stmt.getResultSet();
                if (results != null && results.next()) {
                    inDb = true;
                }

                if (results != null) {
                    results.close();
                }
            }

            stmt.close();
            boolean ok;
            if (inDb) {
                if (mailingdata == null) {
                    stmt = this.connection.prepareStatement("UPDATE `mailing_profile_data` SET `UUID` = ? WHERE `player_name` = ?");
                    stmt.setString(1, UUID);
                    stmt.setString(2, playerName);
                } else {
                    stmt = this.connection.prepareStatement("UPDATE `mailing_profile_data` SET `mailbox` = ?, `UUID` = ? WHERE `player_name` = ?");
                    stmt.setString(1, mailingdata);
                    stmt.setString(2, UUID);
                    stmt.setString(3, playerName);
                }

                ok = stmt.executeUpdate() > 0;
                stmt.close();
                return ok;
            } else {
                if (mailingdata == null) {
                    stmt = this.connection.prepareStatement("INSERT INTO `mailing_profile_data` (`player_name`, `UUID`) VALUES (?, ?)");
                    stmt.setString(1, playerName);
                    stmt.setString(2, UUID);
                } else {
                    stmt = this.connection.prepareStatement("INSERT INTO `mailing_profile_data` (`player_name`, `mailbox`, `UUID`) VALUES (?, ?, ?)");
                    stmt.setString(1, playerName);
                    stmt.setString(2, mailingdata);
                    stmt.setString(3, UUID);
                }

                ok = stmt.executeUpdate() > 0;
                stmt.close();
                return ok;
            }
        }
            
            public MailingData getMailingData(String playerName) {
                try {
                    String[] s = this.getMailingDataFromDb(playerName);
                    if (s != null) {
                        MailingData mailbox = MailingData.deserialize(s[0]);
                        if (s[1] != null) {
                            try {
                                String uniquieID = s[1];
                                int regID = Integer.parseInt(s[2]);
                                mailbox.id = regID;
                                mailbox.UUID = uniquieID;
                            } catch (Exception var11) {
                            }
                        }


                        return mailbox;
                    }
                } catch (SQLException var12) {
                    this.plugin.getLogger().severe("SQL ERROR ON RETRIEVE PLAYER DATA: " + playerName);
                    var12.printStackTrace();
                }

                return null;
            }
            
            private String[] getMailingDataFromDb(String playerName) throws SQLException {
                PreparedStatement stmt = this.connection.prepareStatement("SELECT `mailbox`, `UUID`, 'id' FROM `mailing_profile_data` WHERE `player_name` = ?");
                stmt.setString(1, playerName);
                if (stmt.execute()) {
                    ResultSet results = stmt.getResultSet();
                    if (results != null && results.next()) {
                        String mailbox = results.getString("mailbox");
                        String UUID = results.getString("UUID");
                        String id = results.getString("id");
                        results.close();
                        stmt.close();
                        return new String[]{mailbox, UUID, id};
                    }

                    if (results != null) {
                        results.close();
                    }
                }

                stmt.close();
                return null;
            }

}
