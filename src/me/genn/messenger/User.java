//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package me.genn.messenger;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import me.genn.messenger.ICopyable;
import org.bukkit.entity.Player;

public class User implements ICopyable<User> {
    private UUID ID;
    private String name;
    private Timestamp lastUsed;
    private boolean online;
    private boolean waitingForPermData = true;

    public User(UUID ID, String name) {
        this.ID = ID;
        this.name = name;
        this.lastUsed = Timestamp.from(Instant.now());
    }

    public boolean isOnline() {
        return this.online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public UUID getID() {
        return this.ID;
    }

    public String getName() {
        return this.name;
    }

    public String getNameKey() {
        return this.name.toUpperCase();
    }

    public void setName(String name) {
        this.name = name;
    }



    public Timestamp getLastUsed() {
        return this.lastUsed;
    }

    public void setLastUsed() {
        this.lastUsed = Timestamp.from(Instant.now());
    }

    public User copy() {
        return new User(this.ID, this.name);
    }
}
