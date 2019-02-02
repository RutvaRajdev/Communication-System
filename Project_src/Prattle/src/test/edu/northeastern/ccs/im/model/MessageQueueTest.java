package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueTest {

    private MessageQueue messageQueue = new MessageQueue();

    @Test
    void setQueue() {
        messageQueue = new MessageQueue("rec", new HashMap<>());
        assertEquals(0, messageQueue.getQueue().size());
        Map<String, List<SingleMessage>> message = new HashMap<>();
        message.put("sender", new LinkedList<>());
        messageQueue.setQueue(message);
        assertEquals(1, messageQueue.getQueue().size());
    }

    @Test
    void receiverName() {
        messageQueue = new MessageQueue("rec", new HashMap<>());
        assertEquals("rec", messageQueue.getReceiverName());
        messageQueue.setReceiverName("receiver");
        assertEquals("receiver", messageQueue.getReceiverName());
    }

    @Test
    void testIds() {
        ObjectId id = new ObjectId();
        messageQueue.setId(id);
        assertEquals(id, messageQueue.getId());
    }
}