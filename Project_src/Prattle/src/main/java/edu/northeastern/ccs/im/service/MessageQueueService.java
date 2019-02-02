package edu.northeastern.ccs.im.service;

import edu.northeastern.ccs.im.model.MessageQueue;
import edu.northeastern.ccs.im.model.SingleMessage;

import java.util.List;

/**
 * This service works with "messageQueue" database
 * It provides operations to add a queue, find a queue etc.
 *
 * @author rohanchitnis
 */
public interface MessageQueueService {

    /**
     * Adds given message to the queue
     * @param sender        name of the sender
     * @param receiver      name of the receiver
     * @param message       message to add to queue
     * @return              MessageQueue associated with receiver
     */
    MessageQueue addToQueue(String sender, String receiver, SingleMessage message);

    /**
     * Searches for queue by name of the receiver and returns the same
     */
    MessageQueue searchByReceiver(String receiver);

    /**
     * Removes and returns the queue for a particular sender and removes it from database as the messages are seen
     * @param sender        Name of sender
     * @param receiver      Name of receiver
     * @return              The queue for particular sender and receiver
     */
    List<SingleMessage> getAndRemoveQueue(String sender, String receiver);
}
