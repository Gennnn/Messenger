package me.genn.messenger;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.text.JTextComponent.KeyBinding;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import me.genn.messenger.Database;
import io.netty.buffer.Unpooled;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import net.minecraft.server.v1_8_R3.PacketPlayOutCustomPayload;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;


public class Messenger extends JavaPlugin implements Listener {
		private static Messenger plugin;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss MM/dd/yyyy");
		Set<String> playerDataLoaded;
		public static HashMap<UUID, ArrayList<String>> mailbox = new HashMap<>();
		public static HashMap<UUID, String> nextSend = new HashMap<>();
		public static HashMap<UUID, ItemStack> itemInHand = new HashMap<>();
		private Queue<DatabaseUpdate> updates = new ConcurrentLinkedQueue();
	    private UUID ID;
        private String name;
        private Database database;
        

		
        
        public void onEnable() {
            plugin = this;
            this.playerDataLoaded = new HashSet<String>();
            File dbConfigFile = new File(this.getDataFolder(), "db.yml");
            if (!dbConfigFile.exists()) {
                this.saveResource("db.yml", false);
            }

            YamlConfiguration dbConfig = new YamlConfiguration();

            try {
                dbConfig.load(dbConfigFile);
            } catch (Exception var7) {
                this.getLogger().severe("FAILED TO LOAD DB CONFIG FILE");
                var7.printStackTrace();
                this.setEnabled(false);
                return;
            }
            this.nextSend = new HashMap();
            this.database = new Database(this);
            boolean connected = this.database.connect(dbConfig.getString("database.host"), dbConfig.getString("database.user"), dbConfig.getString("database.pass"), dbConfig.getString("database.db"));
            if (!connected) {
                this.getLogger().severe("DATABASE CONNECTION ERROR, STATS WILL NOT BE SAVED");
                this.database = null;
            } else if (connected) {
            	this.getLogger().severe("AYO WE CONNECTED BAYBEE");
                this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            }

            this.getServer().getPluginManager().registerEvents(this, this);
            this.playerDataLoaded = new HashSet<String>();
        }
		public static BookMeta saveBook(Player player) {
			BookMeta meta = null;
			ItemStack savedBook = player.getItemInHand();
			if (savedBook.getType() == Material.WRITTEN_BOOK) {
				BookMeta meta1 = (BookMeta)savedBook.getItemMeta();
				meta = meta1;
			}
			return meta;
		}
		
		public static void writeBook(Player player) {
			BookMeta meta = null;
			ItemStack blankBook = new ItemStack(Material.BOOK_AND_QUILL);
			ItemStack oldItem = player.getItemInHand();
			player.setItemInHand(blankBook);
	        PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(Unpooled.buffer())); //Create packet that tells the player to open a book
	        CraftPlayer craftPlayer = (CraftPlayer)player;
	        craftPlayer.getHandle().playerConnection.sendPacket(packet);
		}
		
		
		@EventHandler
		public void writeMessage(PlayerEditBookEvent event) {
			BookMeta bookMeta = event.getNewBookMeta();
			Player player = event.getPlayer();
			List<String> text = bookMeta.getPages();
			String title;
			boolean isSigning = event.isSigning();
			if (!isSigning) {
				title = "New Message from " + player.getName();
			} else {
				title = bookMeta.getTitle().toString();
			}
			String senderName = player.getName();
			UUID senderUUID = player.getUniqueId();
			String targetName = nextSend.get(senderUUID);
			sendMessage(senderName, targetName, title, text);
			player.setItemInHand(itemInHand.get(player.getUniqueId()));
		}
		
		@EventHandler
		public void swapOffBook(org.bukkit.event.player.PlayerItemHeldEvent event) {
			int slot = event.getPreviousSlot();
			Player player = event.getPlayer();
			if (player.getInventory().getItem(slot) != (ItemStack)null) {
				if (player.getInventory().getItem(slot).getItemMeta().getDisplayName().contains("Right click") && player.getInventory().getItem(slot) != (ItemStack)null) {
					ItemStack prevItem = itemInHand.get(player.getUniqueId());
				    player.getInventory().setItem(slot, prevItem);
				    player.updateInventory();
				} else if (player.getInventory().getItem(slot) != (ItemStack)null && player.getInventory().getItem(event.getNewSlot()) != (ItemStack)null) {
					return;
			    } else {
			    	return;
			    }
			} else {
				return;
			}
			
		}
		

		
		
		public void sendMessage(String senderName, String targetName, String title, List<String> text) {
			Player sender = Bukkit.getPlayerExact(senderName);
			UUID senderUUID = sender.getUniqueId();
			if (getServer().getOfflinePlayer(targetName).isOnline()) {
				Player target = Bukkit.getPlayerExact(targetName);
				UUID targetUUID = target.getUniqueId();
				LocalDateTime now = LocalDateTime.now();
				List<String> currentMailbox = mailbox.get(targetUUID);
				Message message = new Message();
				message.receiverUUID = targetUUID.toString();
				message.senderUUID = senderUUID.toString();
				message.title = title;
				message.sendTime = now.toString();
				message.text = text;
				plugin.database.saveMessage(target.getName(), message);
			} else {
				OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
				UUID targetUUID = target.getUniqueId();
				LocalDateTime now = LocalDateTime.now();
				List<String> currentMailbox = mailbox.get(targetUUID);
				Message message = new Message();
				message.receiverUUID = targetUUID.toString();
				message.senderUUID = senderUUID.toString();
				message.title = title;
				message.sendTime = now.toString();
				message.text = text;
				plugin.database.saveMessage(target.getName(), message);
			}
			
			
			
			
		}
		public void readMessage(UUID sender, int messageNumber) {
			int messageNumber1 = messageNumber;
			Player player = Bukkit.getPlayer(sender);
			player.sendMessage(messageNumber1 + "");
			final UUID id = sender;
			player.sendMessage(id.toString());
			String message = "";
			List<String> messages = new ArrayList<String>();
			messages = mailbox.get(player.getUniqueId());
			player.sendMessage(messages.toString());
			if (messages == null) {
				ArrayList<String> newMessages = new ArrayList<String>();
				player.sendMessage("Mailbox for id " + id + " is null.");
				return;
			} else {
				ArrayList<String> newMessages = new ArrayList<String>();
				newMessages = (ArrayList<String>) messages;
				
			}

			ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
			BookMeta meta = (BookMeta)book.getItemMeta();
			String[] str = new String[messages.size()];
			messages.toArray(str);
			message = str[messageNumber];
			if ((messages.get(0)).toString() instanceof String) {
				message = (messages.get(messageNumber1)).toString();
				String[] msgSp1 = message.split("title:", 2);
				String[] msgSp2 = msgSp1[1].split("from:", 2);
				meta.setTitle(msgSp2[0]);
				String[] msgSp3 = msgSp2[1].split("text:", 2);
				meta.setAuthor(msgSp3[0]);
				String[] msgSp4 = msgSp3[1].split("sendtime:", 2);
				meta.setPages(msgSp4[0]);
				ItemStack oldItem = player.getItemInHand(); //Get item in hand so we can set it back
		        player.setItemInHand(book); //Set item in hand to book
		        PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(Unpooled.buffer())); //Create packet that tells the player to open a book
		        CraftPlayer craftPlayer = (CraftPlayer)player; //Craftplayer for sending packet
		        craftPlayer.getHandle().playerConnection.sendPacket(packet); //Send packet
		        player.setItemInHand(oldItem);
			} else {
				player.sendMessage("Somehow it is not a string??");
			}
			
			
		}
		
		
		public static void openBook(Player player)
	    {
	        ItemStack book = new ItemStack(Material.WRITTEN_BOOK); //Create book ItemStack
	        BookMeta meta = (BookMeta)book.getItemMeta(); //Get BookMeta
	        meta.addPage("Hello!"); //Add a page
	        book.setItemMeta(meta); //Set meta
	        ItemStack oldItem = player.getItemInHand(); //Get item in hand so we can set it back
	        player.setItemInHand(book); //Set item in hand to book
	        PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(Unpooled.buffer())); //Create packet that tells the player to open a book
	        CraftPlayer craftPlayer = (CraftPlayer)player; //Craftplayer for sending packet
	        craftPlayer.getHandle().playerConnection.sendPacket(packet); //Send packet
	        player.setItemInHand(oldItem); //Set item in hand back
	    }
		
		public static void openBookBlank(Player player, String targetName)
	    {
	        ItemStack book = new ItemStack(Material.BOOK_AND_QUILL); //Create book ItemStack
	        ItemMeta meta = book.getItemMeta(); //Get BookMet
	        ItemStack oldItem;
	        if (player.getItemInHand() != null) {
	        	oldItem = player.getItemInHand();
	        } else {
	        	oldItem = new ItemStack(Material.AIR);
	        }
	         //Get item in hand so we can set it back
	        meta.setDisplayName(ChatColor.GRAY.toString() + ChatColor.ITALIC.toString() + "Right click to write a new message to: " + ChatColor.RESET.toString() + ChatColor.WHITE.toString() + targetName);
	        ((BookMeta) meta).addPage("");
	        ((BookMeta) meta).addPage("");
	        book.setItemMeta(meta);
	        player.setItemInHand(book); //Set item in hand to book
	        
	        itemInHand.put(player.getUniqueId(), oldItem);
	    }
		
		public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
	        if (command.getName().equals("pm") && sender instanceof Player) {
	        	if (args.length > 0) {
	        		UUID UUIDsender = ((Player)sender).getUniqueId();
	        		String nextTarget = args[0];
	        		nextSend.put(UUIDsender, nextTarget);
	        		openBookBlank((Player) sender, nextTarget);
	        	} else {
	        		sender.sendMessage(ChatColor.RED + "You must specify a player to send a message to!");
	        	}
	            
	        } else if (command.getName().equals("read") && sender instanceof Player) {
	        	if (args.length > 0) {
	        		UUID UUIDsender = ((Player)sender).getUniqueId();
	        		int messageRead = Integer.parseInt(args[0]);
	        		sender.sendMessage(UUIDsender + ", " + messageRead);
	        		readMessage(UUIDsender, messageRead);
	        	} else {
	        		sender.sendMessage(ChatColor.RED + "You must specify a player to send a message to!");
	        	}
	        }

	        return true;
	    }
		
		
		
		
		
		public static void saveMailingData(Player player) {
	        if (plugin.database != null) {
	            final String playerName = player.getName();
	            final UUID ID = player.getUniqueId();
	            MailingData mailingdata = new MailingData();
	            mailingdata.messages = mailbox.get(ID);
	            plugin.database.saveMailingData(playerName, ID, mailingdata);
	            
	        }

	    }
		
		public static MailingData getMailingData(Player player) {
			MailingData data = null;
	        if (plugin.database != null) {
	            final String playerName = player.getName();
	            final UUID ID = player.getUniqueId();
	            data = plugin.database.getMailingData(playerName);
	            return data;
	            
	        }
	        return data;

	    }
		
		public static List<Message> getMessages(Player player) {
			
	        if (plugin.database != null) {
	            final String playerName = player.getName();
	            List<Message> messages = plugin.database.getPlayerMessages(playerName);
	            return messages;
	            
	        }
			return null;

	    }
		
	
		

		
		

		 

}
