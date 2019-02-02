package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * This class represents a single message with its type
 * @author rohanchitnis
 */
public class SingleMessage implements Serializable {

    private ObjectId id;
    private String message;
    private MessageType type;
    private String path;
    private String senderIp;
    private List<String> receiverIp;
    private String sender;
    private Date timestamp;

    private static final long serialVersionUID = 1L;

    public SingleMessage() {
        this.id = new ObjectId();
    }

    /**
     * Constructor
     * @param message       Message as String
     * @param type          Type of the message
     */
    public SingleMessage(String message, MessageType type) {
        this.message = message;
        this.type = type;
        this.timestamp = new Date();
        this.id = new ObjectId();
    }

    /**
     * Returns the id
     * @return  the id
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Sets the id
     * @param id    the id to set
     */
    public void setId(ObjectId id) {
        this.id = id;
    }

    /**
     * Gets the timestamp
     * @return  the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the time stamp
     * @param timestamp the time stamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return              Message sender name
     */
    public String getSender() {
        return sender;
    }

    /**
     * Sets the sender name with given name
     * @param sender
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * @return      IP address of the sender of this message
     */
    public String getSenderIp() {
        return senderIp;
    }

    /**
     * Sets the sender IP address with given ip
     */
    public void setSenderIp(String senderIp) {
        this.senderIp = senderIp;
    }

    /**
     * @return      IP address of the receiver of this message
     */
    public List<String> getReceiverIp() {
        return receiverIp;
    }

    /**
     * Sets the receiver IP address with given ip
     */
    public void setReceiverIp(List<String> receiverIp) {
        this.receiverIp = receiverIp;
    }

    /**
     * @return              The path of the file (for MIME message type)
     */
    public String getPath() {
        return path;
    }

    /**
     *
     * @param path          Sets the path to given path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return              Message as String
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message with given message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return              Type of the message
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Sets the type of the message
     */
    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SingleMessage{" +
                "message='" + message + '\'' +
                ", type=" + type +
                '}';
    }
}
