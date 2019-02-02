package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

/**
 * MessageQueue class to store the message queue in the database.
 * This queue will be filled if the receiver of the message is not online.
 *
 * @author rohanchitnis
 */
public class MessageQueue {

    private static final long serialVersionUID = 1L;
    private ObjectId id;
    private String receiverName;
    private Map<String, List<SingleMessage>> queue;

    public MessageQueue() {
        this.id = new ObjectId();
    }

    /**
     * Constructor
     * @param receiverName      Given receiver name
     * @param queue             Given queue
     */
    public MessageQueue(String receiverName, Map<String, List<SingleMessage>> queue) {
        this.receiverName = receiverName;
        this.queue = queue;
    }

    /**
     * @return      Message queue
     */
    public Map<String, List<SingleMessage>> getQueue() {
        return queue;
    }

    /**
     * Sets the message queue with given queue
     * @param queue
     */
    public void setQueue(Map<String, List<SingleMessage>> queue) {
        this.queue = queue;
    }

    /**
     * @return      The receiver of this queue
     */
    public String getReceiverName() {
        return receiverName;
    }

    /**
     * Sets the receiver of this queue
     * @param receiverName     The given receiver name
     */
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    /**
     * Returns the Id of a particular queue
     * @return Queue Id in the form of ObjectId object
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Sets the ID of queue
     * @param id    the ObjectId to set
     */
    public void setId(ObjectId id) {
        this.id = id;
    }
}
