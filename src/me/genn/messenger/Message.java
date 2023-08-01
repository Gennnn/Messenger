

package me.genn.messenger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class Message {
    public List<String> text;
    public String senderUUID;
    public String receiverUUID;
    public String sendTime;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss MM/dd/yyyy");
    public String title;

    public Message() {
    }

    public String serialize() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("senderUUID", this.senderUUID);
        config.set("receiverUUID", this.receiverUUID);
        config.set("text", Arrays.asList(this.text));
        config.set("time", this.sendTime);
        config.set("title", this.title);
        return config.saveToString();
    }

    public static Message deserialize(String s) {
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.loadFromString(s);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }

        Message message = new Message();
        message.senderUUID = config.getString("senderUUID");
        message.receiverUUID = config.getString("receiverUUID");
        message.text = (List<String>) config.getList("text", new ArrayList());
        message.sendTime = config.getString("time");
        message.title = config.getString("title");
        return message;
    }
    
    
}
