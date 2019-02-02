package edu.northeastern.ccs.im.service;

import edu.northeastern.ccs.im.model.ChatMessage;
import edu.northeastern.ccs.im.model.MessageType;
import edu.northeastern.ccs.im.model.SingleMessage;

import java.util.Date;
import java.util.List;

/**
 * Interface to work with Message database
 * @author rohanchitnis
 */
public interface MessageService {

    /**
     * Saves message to database
     * @param groupName         Name of the group / sender$receiver
     * @param message           Message as String
     * @param type              Message type as specified in MessageType enum
     * @param sender            Name of the sender
     * @return                  Newly created message object
     */
    ChatMessage createMessage(String groupName, String message, MessageType type, String senderIP,
                              List<String> receiverIP, String sender);

    /**
     * Finds and returns ChatMessage for given group name
     * @return                  Group name if found, null otherwise
     */
    ChatMessage findMessage(String groupName);

    /**
     * Searches for messages with given name as either sender or receiver
     * This is for CALEA compliance where any authority can ask for messages involving one person
     * @param name              name to search for
     * @return                  List of messages where the sender or receiver is the given name
     */
    List<SingleMessage> searchMessages(String name);

    /**
     * Searches for messages between 2 timestamps
     * @param time1             Start time
     * @param time2             End time
     * @param groupName         group name to retrieve messages from
     * @return                  List of messages between 2 given times
     */
    List<SingleMessage> searchMessages(Date time1, Date time2, String groupName);
}
