//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package me.genn.messenger;

public class MessageUpdate implements DatabaseUpdate {
    public String playerName;
    public Message message;

    public MessageUpdate(String receiverUUID, Message message) {
        this.playerName = playerName;
        this.message = message;
    }
}
