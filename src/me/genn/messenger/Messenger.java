package me.genn.messenger;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.text.JTextComponent.KeyBinding;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import me.genn.messenger.commands.FriendGotoCommand;
import me.genn.messenger.commands.FriendListCommand;
import me.genn.messenger.commands.FriendRemCommand;
import me.genn.chatcontrol.ChatControl;
import me.genn.gennsgym.GennsGym;
import me.genn.gennsgym.Upgrades;
import me.genn.messenger.commands.FriendAddCommand;
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
		public ChatColor chatColorMain;
		public ChatColor chatColorHighlight;
		public Map<String, List<String>> onlineFriends;
		public Map<String, List<String>> allFriends;
		private static Messenger plugin;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss MM/dd/yyyy");
		Set<String> playerDataLoaded;
		Map<String, List<Message>> mailbox = new HashMap();
		public static HashMap<UUID, String> nextSend = new HashMap<>();
		public static HashMap<UUID, ItemStack> itemInHand = new HashMap<>();
		private Queue<DatabaseUpdate> updates = new ConcurrentLinkedQueue();
	    private UUID ID;
        private String name;
        public Database database;
        

		
        
        public void onEnable() {
            plugin = this;
            this.database = new Database(GennsGym.getDatabaseConnection());
            this.getCommand("friendadd").setExecutor(new FriendAddCommand(this));
            this.getCommand("friendlist").setExecutor(new FriendListCommand(this));
            FriendRemCommand remCmd = new FriendRemCommand(this);
            this.getCommand("friendrem").setExecutor(remCmd);
            this.getCommand("friendrem").setTabCompleter(remCmd);
            FriendGotoCommand gotoCmd = new FriendGotoCommand(this);
            this.getCommand("friendgoto").setExecutor(gotoCmd);
            this.getCommand("friendgoto").setTabCompleter(gotoCmd);
            this.nextSend = new HashMap();    
            this.mailbox = new HashMap();
            this.getServer().getPluginManager().registerEvents(this, this);
            
            
            Player[] var5;
            int var4 = (var5 = Bukkit.getOnlinePlayers().toArray(new Player[ Bukkit.getOnlinePlayers().size() ])).length;

            for(int var3 = 0; var3 < var4; ++var3) {
                Player player = var5[var3];
                this.loadMessages(player);
            }
            
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
			if (player.getInventory().getItem(slot) != null) {
				if (player.getInventory().getItem(slot) != null && player.getInventory().getItem(slot).hasItemMeta() && player.getInventory().getItem(slot).getItemMeta().getDisplayName().contains("Right click") && player.getInventory().getItem(slot).getAmount() > 0 ) {
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
			List<String> ignorers = ChatControl.ignoreList(senderName);
            if (ignorers != null && ignorers.contains(targetName.toLowerCase())) {
                ActionBarUtil.sendMessage(Bukkit.getPlayerExact(senderName), ChatColor.RED + "You cannot message that person.");
            } else {
            	if (ignorers == null) {
            		sender.sendMessage("Ignorers: null");
            	} else {
            		sender.sendMessage("Ignorers: " + ignorers.toString());
            	}
            	if (getServer().getOfflinePlayer(targetName).isOnline()) {
    				sendMessageToDatabase(senderUUID.toString(), targetName, title, text, true);
    			} else {
    				sendMessageToDatabase(senderUUID.toString(), targetName, title, text, false);
    			}
            }
			
		}
			
			
		public void sendMessageToDatabase(String senderUUID, String targetName, String title, List<String> text, boolean online) {
			OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
			UUID targetUUID = target.getUniqueId();
			LocalDateTime now = LocalDateTime.now();
			Message message = new Message();
			message.receiverUUID = targetUUID.toString();
			message.senderUUID = senderUUID.toString();
			message.title = title;
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss");
			message.sendTime = dtf.format(now).toString();
			message.text = text;
			plugin.database.saveMessage(target.getName(), message);
			if (online) {
				List<Message> currentMailbox = mailbox.get(targetUUID.toString());
				currentMailbox.add(0, message);
				mailbox.replace(targetUUID.toString(), currentMailbox);
			}
		}
		
		
		public void readMessage(Player player, Message message) {
					
			ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
			BookMeta meta = (BookMeta)book.getItemMeta();
			meta.setTitle(player.getName());
			meta.setAuthor(player.getName());
			List<String> text = message.text;
			meta.setPages(text);
			ItemStack oldItem = player.getItemInHand(); //Get item in hand so we can set it back
			book.setItemMeta(meta);
		    player.setItemInHand(book); //Set item in hand to book
		    PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(Unpooled.buffer())); //Create packet that tells the player to open a book
		    CraftPlayer craftPlayer = (CraftPlayer)player; //Craftplayer for sending packet
		    craftPlayer.getHandle().playerConnection.sendPacket(packet); //Send packet
		    player.setItemInHand(oldItem);
			
			
			
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
	        book.setItemMeta(meta);
	        player.setItemInHand(book); //Set item in hand to book
	        
	        itemInHand.put(player.getUniqueId(), oldItem);
	    }
		
		public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
	        if (command.getName().equals("mail") && sender instanceof Player) {
	        	if (args.length > 0) {
	        		UUID UUIDsender = ((Player)sender).getUniqueId();
	        		String nextTarget = args[0];
	        		nextSend.put(UUIDsender, nextTarget);
	        		openBookBlank((Player) sender, nextTarget);
	        	} else {
	        		sender.sendMessage(ChatColor.RED + "You must specify a player to send a message to!");
	        	}
	            
	       
	        } else if (command.getName().equals("mailbox") && sender instanceof Player) {
	        	if (this.database == null) {
	        		sender.sendMessage(ChatColor.RED + "Error.");
	        	} else if (args.length > 0) {
	        		this.openMailbox((Player) sender, "Mailbox", (Inventory)null, Integer.parseInt(args[0]));
	        	} else if (args.length == 0){
	        		this.openMailbox((Player) sender, "Mailbox", (Inventory)null, 1);
	        		
	        	} else {
	        		sender.sendMessage(ChatColor.RED + "You must specify a player to send a message to!");
	        	}
	        }

	        return true;
	    }
		
		
		public void openMailbox(final Player player, final String name, final Inventory inventory, int page) {
	    	ItemStack cornerItem = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
	    	SkullMeta cornerMeta = (SkullMeta) cornerItem.getItemMeta();
	    	cornerMeta.setDisplayName(ChatColor.WHITE.toString() + player.getName() + "'s Mailbox");
	    	cornerMeta.setOwner(player.getName());
	    	cornerItem.setItemMeta(cornerMeta);
	    	ItemStack borderItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 7);
	    	ItemMeta borderMeta = borderItem.getItemMeta();
	    	ItemStack closeItem = new ItemStack(Material.INK_SACK, 1, (byte) 2);
	    	ItemMeta closeMeta = closeItem.getItemMeta();
	    	closeMeta.setDisplayName(ChatColor.RED.toString() + "Cancel");
	    	closeItem.setItemMeta(closeMeta);
	    	borderMeta.setDisplayName(ChatColor.GRAY.toString() + "");
	    	borderItem.setItemMeta(borderMeta);
	    	
	    	
	    	
	    	
	    	
	    	int size = 54;

	        
	        final Inventory inv = inventory != null ? inventory : Bukkit.createInventory(player, size, "Messages");
	        inv.setItem(0, cornerItem);
	        for (int i = 1; i < 9; i++) {
	        	inv.setItem(i, borderItem);
	        }
	        for (int i = 9; i < size-9; i=i+9) {
	        	inv.setItem(i, borderItem);
	        }
	        for (int i = 8; i < size-9; i=i+9) {
	        	inv.setItem(i, borderItem);
	        }
	        for (int i = size-9; i < size-1; i++) {
	        	inv.setItem(i, borderItem);
	        }
	        inv.setItem(53, closeItem);
	        List<Message> messages = this.mailbox.get(player.getUniqueId().toString());
	        int pages = (messages.size()/28) + 1;
	        int remainder = (messages.size() % 28);
	        if (pages - (messages.size()/28) == 1) {
	        	for (int i = 0; i < remainder; i++) {
	        		Message message = messages.get(i + ((page-1) * 28));
	        		ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
	        		SkullMeta headMeta = (SkullMeta) head.getItemMeta();
	        		if (Bukkit.getOfflinePlayer(UUID.fromString(message.senderUUID)).isOnline()) {
	        			headMeta.setOwner((Bukkit.getPlayer(UUID.fromString(message.senderUUID)).getName()));
	        			if (message.read == 1) {
	        				headMeta.setDisplayName(ChatColor.GRAY.toString() + message.title);
	        			} else if (message.read == 0) {
	        				headMeta.setDisplayName(ChatColor.WHITE.toString() + message.title);
	        			}
	        			List<String> lore = new ArrayList<>();
	        			String trimmedTime = message.sendTime.substring(0, message.sendTime.length() - 3);
	        			String[] date = trimmedTime.split(" ");
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[0]);
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[1]);
	        			lore.add(ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + message.sendTime);
	        			headMeta.setLore(lore);
	        			head.setItemMeta(headMeta);
	        			int pos = i - (28 * (page - 1));
	        			if (pos < 7) {
	        				int slot = pos + 10;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 7 && pos < 14) {
	        				int slot = pos+12;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 14 && pos < 21) {
	        				int slot = pos+14;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 21 && pos < 28) {
	        				int slot = pos+16;
	        				inv.setItem(slot, head);
	        			}
	        		} else {
	        			headMeta.setOwner((Bukkit.getOfflinePlayer(UUID.fromString(message.senderUUID)).getName()));
	        			if (message.read == 1) {
	        				headMeta.setDisplayName(ChatColor.GRAY.toString() + message.title);
	        			} else if (message.read == 0) {
	        				headMeta.setDisplayName(ChatColor.WHITE.toString() + message.title);
	        			}
	        			List<String> lore = new ArrayList<>();
	        			String trimmedTime = message.sendTime.substring(0, message.sendTime.length() - 3);
	        			String[] date = trimmedTime.split(" ");
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[0]);
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[1]);
	        			lore.add(ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + message.sendTime);
	        			headMeta.setLore(lore);
	        			head.setItemMeta(headMeta);
	        			int pos = i - (28 * (page - 1));
	        			if (pos < 7) {
	        				int slot = pos + 10;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 7 && pos < 14) {
	        				int slot = pos+12;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 14 && pos < 21) {
	        				int slot = pos+14;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 21 && pos < 28) {
	        				int slot = pos+16;
	        				inv.setItem(slot, head);
	        			}
	        		}
	        		
	        	}
	        } else {
	        	for (int i = 0; i < 28; i++) {
	        		Message message = messages.get(i + ((page-1) * 28));
	        		ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
	        		SkullMeta headMeta = (SkullMeta) head.getItemMeta();
	        		if (Bukkit.getOfflinePlayer(UUID.fromString(message.senderUUID)).isOnline()) {
	        			headMeta.setOwner((Bukkit.getPlayer(UUID.fromString(message.senderUUID)).getName()));
	        			if (message.read == 1) {
	        				headMeta.setDisplayName(ChatColor.GRAY.toString() + message.title);
	        			} else if (message.read == 0) {
	        				headMeta.setDisplayName(ChatColor.WHITE.toString() + message.title);
	        			}
	        			
	        			List<String> lore = new ArrayList<>();
	        			String trimmedTime = message.sendTime.substring(0, message.sendTime.length() - 3);
	        			String[] date = trimmedTime.split(" ");
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[0]);
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[1]);
	        			lore.add(ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + message.sendTime);
	        			headMeta.setLore(lore);
	        			head.setItemMeta(headMeta);
	        			int pos = i - (28 * (page - 1));
	        			if (pos < 7) {
	        				int slot = pos + 10;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 7 && pos < 14) {
	        				int slot = pos+12;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 14 && pos < 21) {
	        				int slot = pos+14;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 21 && pos < 28) {
	        				int slot = pos+16;
	        				inv.setItem(slot, head);
	        			}
	        		} else {
	        			headMeta.setOwner((Bukkit.getOfflinePlayer(UUID.fromString(message.senderUUID)).getName()));
	        			if (message.read == 1) {
	        				headMeta.setDisplayName(ChatColor.GRAY.toString() + message.title);
	        			} else if (message.read == 0) {
	        				headMeta.setDisplayName(ChatColor.WHITE.toString() + message.title);
	        			}
	        			List<String> lore = new ArrayList<>();
	        			String trimmedTime = message.sendTime.substring(0, message.sendTime.length() - 3);
	        			String[] date = trimmedTime.split(" ");
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[0]);
	        			lore.add(ChatColor.DARK_GRAY.toString() + date[1]);
	        			lore.add(ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + message.sendTime);
	        			headMeta.setLore(lore);
	        			head.setItemMeta(headMeta);
	        			int pos = i - (28 * (page - 1));
	        			if (pos < 7) {
	        				int slot = pos + 10;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 7 && pos < 14) {
	        				int slot = pos+12;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 14 && pos < 21) {
	        				int slot = pos+14;
	        				inv.setItem(slot, head);
	        			} else if (pos >= 21 && pos < 28) {
	        				int slot = pos+16;
	        				inv.setItem(slot, head);
	        			}
	        		}
		        }
	        }
	        
	        	
	        
	        if (inventory == null) {
	            Bukkit.getScheduler().scheduleSyncDelayedTask(Messenger.this.plugin, new Runnable() {
	                public void run() {
	                    player.openInventory(inv);

	                }
	            }, 6L);
	        }

	        
	               
	    }
		
		public void openMoreMailOptions(final Player player, final Inventory inventory, Message message) {


	    	ItemStack borderItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 7);
	    	ItemMeta borderMeta = borderItem.getItemMeta();
	    	ItemStack closeItem = new ItemStack(Material.INK_SACK, 1, (byte) 2);
	    	ItemMeta closeMeta = closeItem.getItemMeta();
	    	closeMeta.setDisplayName(ChatColor.RED.toString() + "Cancel");
	    	closeItem.setItemMeta(closeMeta);
	    	borderMeta.setDisplayName(ChatColor.GRAY.toString() + "");
	    	borderItem.setItemMeta(borderMeta);
	    	ItemStack readItem = this.menuItem(new ItemStack(Material.INK_SACK, 1, (byte) 1), ChatColor.AQUA.toString() + "Mark as Read", new ArrayList<String>(Arrays.asList(ChatColor.DARK_GRAY.toString() + "Click to mark message as read.")));
	    	ItemStack replyItem = this.menuItem(new ItemStack(Material.BOOK_AND_QUILL), ChatColor.GREEN.toString() + "Reply to Message", new ArrayList<String>(Arrays.asList(ChatColor.DARK_GRAY.toString() + "Click to reply to this message.")));
	    	ItemStack ignoreItem = this.menuItem(new ItemStack(Material.IRON_FENCE), ChatColor.RED.toString() + "Block Sender", new ArrayList<String>(Arrays.asList(ChatColor.DARK_GRAY.toString() + "Prevent the user " + Bukkit.getOfflinePlayer(UUID.fromString(message.senderUUID)).getName() + " from contacting you.")));
	    	ItemStack trashItem = this.menuItem(new ItemStack(Material.HOPPER), ChatColor.GRAY.toString() + "Delete Message", new ArrayList<String>(Arrays.asList(ChatColor.DARK_GRAY.toString() + "Delete this message from your mailbox.")));

	    	
	    	
	    	
	    	int size = 27;

	        
	        final Inventory inv = inventory != null ? inventory : Bukkit.createInventory(player, size, "Message Options");
	        inv.setItem(0, borderItem);
	        for (int i = 1; i < 9; i++) {
	        	inv.setItem(i, borderItem);
	        }
	        for (int i = 9; i < size-9; i=i+9) {
	        	inv.setItem(i, borderItem);
	        }
	        for (int i = 8; i < size-9; i=i+9) {
	        	inv.setItem(i, borderItem);
	        }
	        for (int i = size-9; i < size-1; i++) {
	        	inv.setItem(i, borderItem);
	        }
	        inv.setItem(size-1, closeItem);
	        
	        inv.setItem(10, readItem);
	        inv.setItem(12, replyItem);
	        inv.setItem(14, ignoreItem);
	        inv.setItem(16, trashItem);
	        
	        	
	        
	        if (inventory == null) {
	            Bukkit.getScheduler().scheduleSyncDelayedTask(Messenger.this.plugin, new Runnable() {
	                public void run() {
	                    player.openInventory(inv);

	                }
	            }, 6L);
	        }

	        
	               
	    }
		
		public ItemStack menuItem(ItemStack item, String name, List<String> lore) {
	    	ItemMeta itemMeta = item.getItemMeta();
	    	itemMeta.setDisplayName(name);
	    	lore.add(ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + ChatColor.stripColor(lore.get(lore.size()-1)));
	    	itemMeta.setLore(lore);
	    	item.setItemMeta(itemMeta);
	    	return item;
		}
		
		@EventHandler
	    public void onInventoryClick(InventoryClickEvent event) {
	        if (event.getInventory().getTitle().startsWith("Messages")) {
	        	ItemStack item = event.getCurrentItem();
	        	if (item != null && item.getType() == Material.SKULL_ITEM && item.hasItemMeta()) {
	        		if (event.getClick().isLeftClick()) {
	        			if (ChatColor.stripColor(item.getItemMeta().getDisplayName()).equalsIgnoreCase( ((Player)event.getWhoClicked()).getName() + "'s Mailbox") && item.getItemMeta().hasLore()) {
		        			event.setCancelled(true);
		        		} else {
		        			final Player player = (Player)event.getWhoClicked();
			        		SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
			        		String skullOwnerID = (Bukkit.getOfflinePlayer(skullMeta.getOwner())).getUniqueId().toString();
			        		List<String> lore = skullMeta.getLore();
			        		String dateTime = ChatColor.stripColor(lore.get(2));
			        		Iterator iter =  mailbox.get((player.getUniqueId()).toString()).iterator();
			        		
			        		while(iter.hasNext()) {
			        			Message checkMsg = (Message) iter.next();
			        			if (checkMsg.sendTime.equals(dateTime) && checkMsg.senderUUID.equals(skullOwnerID) && checkMsg.receiverUUID.equals((player.getUniqueId()).toString())) {
			        				List<Message> newMailbox = mailbox.get((player.getUniqueId()).toString());
			        				if (newMailbox.contains(checkMsg)) {
					        			int index = newMailbox.indexOf(checkMsg);
					        			if (index != -1) {
					        				checkMsg.read = 1;
							        		newMailbox.set(index, checkMsg);
							        		mailbox.replace(player.getUniqueId().toString(), newMailbox);
							        		final Message msgToRead = checkMsg;
							        		Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
							                    public void run() {

							                    	Messenger.plugin.readMessage(player, checkMsg);

							                    	
							                    }
							                }, 1L);
						        		} else {
						        			Message message = plugin.database.getPlayerMessage(player.getName(), dateTime, skullOwnerID);

							        		boolean ok = plugin.database.updatePlayerReadMessage(player.getName(), message);
							        		Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
							                    public void run() {
							                    	Messenger.plugin.readMessage(player, message);
							                    }
							                }, 1L);
						        		}
					        		} else {
					        			player.sendMessage("Mailbox doesn't contain checkMsg");
					        			Message message = plugin.database.getPlayerMessage(player.getName(), dateTime, skullOwnerID);
						        		boolean ok = plugin.database.updatePlayerReadMessage(player.getName(), message);
						        		Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
						                    public void run() {
						                    	Messenger.plugin.readMessage(player, message);
						                    }
						                }, 1L);
					        		}
			        			} else {
			        			}
			        		}
			        		
			                
		        		}
	        		
	        		} else if (event.getClick().isRightClick()) {
	        			final Player player = (Player)event.getWhoClicked();
		        		SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
		        		String skullOwnerID = (Bukkit.getOfflinePlayer(skullMeta.getOwner())).getUniqueId().toString();
		        		List<Message> currentMailbox = mailbox.get(player.getUniqueId().toString());
		        		List<String> lore = skullMeta.getLore();
		        		String dateTime = ChatColor.stripColor(lore.get(2));
		        		Iterator iter = currentMailbox.iterator();
		        		while (iter.hasNext()) {
		        			Message message = (Message) iter.next();
		        			if (message.sendTime.equals(dateTime) && message.receiverUUID.equals(player.getUniqueId().toString()) && message.senderUUID.equals(skullOwnerID))  {
		        				Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
				                    public void run() {
				                    	Messenger.plugin.openMoreMailOptions(player, (Inventory)null, message);
				                    }
				                }, 1L);
		        			}
		        		}
		                
	        		} else {
	        			event.setCancelled(true);
	        		}
	        		
	        		
	        	} else {
	        		event.setCancelled(true);
	        	}
	            
	        	event.setCancelled(true);
	        } else if (event.getInventory().getTitle().startsWith("Message Options")) {
	        	ItemStack item = event.getCurrentItem();
	        	if (item != null && item.getType() == Material.SKULL_ITEM && item.hasItemMeta()) {
	        			if (ChatColor.stripColor(item.getItemMeta().getDisplayName()).equalsIgnoreCase(ChatColor.stripColor("Mark as Read")) && item.getItemMeta().hasLore()) {
		        			event.setCancelled(true);
		        		} else {
		        			final Player player = (Player)event.getWhoClicked();
			        		SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
			        		String skullOwnerID = (Bukkit.getOfflinePlayer(skullMeta.getOwner())).getUniqueId().toString();
			        		List<String> lore = skullMeta.getLore();
			        		String dateTime = ChatColor.stripColor(lore.get(2));
			        		Message message = plugin.database.getPlayerMessage(player.getName(), dateTime, skullOwnerID);
			        		boolean ok = plugin.database.updatePlayerReadMessage(player.getName(), message);
			                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
			                    public void run() {
			                    	Messenger.plugin.readMessage(player, message);
			                    }
			                }, 1L);
		        		}
	        		}
	        	
	        		
	        }
	        
	        
	    }
		
		
		
		public List<Message> getMessages(Player player) {
			
	        if (this.plugin.database != null) {
	            final String playerName = player.getName();
	            List<Message> messages = plugin.database.getPlayerMessages(playerName);
	            return messages;
	            
	        } else {
	        	return new ArrayList();
	        }
			

	    }
		
		@EventHandler
	    public void onJoin(final PlayerJoinEvent event) {
	        
	        final Player player = event.getPlayer();
	        String playerName = player.getName();
	        int count = this.plugin.database.getPlayerUnalertedMessages(playerName);
	        if (count > 0) {
	        	ActionBarUtil.sendMessage(player, ChatColor.GOLD + "You have " + ChatColor.YELLOW + count + ChatColor.GOLD + " unread messages.");
	        }
	        
		}
		
		List<Message> getMessagesJoin(Player player) {
	        return this.database != null ? this.database.getPlayerMessages(player.getName()) : null;
	    }
		
		private void loadMessages(final Player player) {
	        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
	            public void run() {
	                List<Message> s = Messenger.this.plugin.getMessagesJoin(player);
	                if (s != null) {
	                    Messenger.this.setMessages(player, s);
	                }

	            }
	        });
	    }
		private void setMessages(final Player player, final List<Message> s) {
	        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
	            public void run() {
	                Messenger.this.mailbox.put(player.getUniqueId().toString(), s);
	            }
	        });
	    }
		@EventHandler(
		        priority = EventPriority.LOWEST
		    )
		    void onJoinMessageLoad(PlayerJoinEvent event) {
		        this.loadMessages(event.getPlayer());
		    }

}
