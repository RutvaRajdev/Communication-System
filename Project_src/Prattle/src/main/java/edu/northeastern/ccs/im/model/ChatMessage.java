package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * A class that implements the functionality related to a chat message such as getting ID, getting message, etc.
 * @author rohanchitnis
 */
public class ChatMessage implements Serializable {

    private ObjectId id;
    private List<SingleMessage> messages = new LinkedList<>();
    // for message between group this field contains groupname
    // for one to one message this field contains "sendername$receiverName"
    private String groupName;

    private static final long serialVersionUID = 1L;

    /**
     * A non-parameterised constructor that creates a chat
     * message with a random ID and no message
     */
    public ChatMessage() {
        this.id = new ObjectId();
    }

    /**
     * A parameterised constructor that creates a message with
     * given group ID, message and type and sets s random message ID
     * @param messages          the messages as list of SingleMessage
     * @param groupName         the group name
     */
    public ChatMessage(List<SingleMessage> messages, String groupName) {
        this.id = new ObjectId();
        this.messages = messages;
        this.groupName = groupName;
    }

    /**
     * Returns the Id of a particular message
     * @return Message Id in the form of ObjectId object
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Sets the ID of chat message
     * @param id    the ObjectId to set
     */
    public void setId(ObjectId id) {
        this.id = id;
    }

    /**
     * Returns the message in form of list
     * @return the list of messages
     */
    public List<SingleMessage> getMessages() {
        return messages;
    }

    /**
     * Sets the messages
     * @param messages       the list of messages to set
     */
    public void setMessages(List<SingleMessage> messages) {
        this.messages = messages;
    }

    /**
     * Returns the Id of the group the chat message belongs to
     * @return Group name as a String
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the group ID for chat message
     * @param groupName       the String representing the group name
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
