

package me.genn.messenger;

import java.util.ArrayList;
import java.util.Arrays;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class MailingData {
    public ArrayList<String> messages = null;
    public String UUID = null;
	public int id = 0;
    public MailingData() {
    }

    public String serialize() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("messages", Arrays.asList(this.messages));
        return config.saveToString();
    }

    public static MailingData deserialize(String s) {
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.loadFromString(s);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }

        MailingData maildata = new MailingData();
        maildata.messages = ((ArrayList<String>)config.getList("messages", new ArrayList<String>()));
        return maildata;
    }
    
    
}
