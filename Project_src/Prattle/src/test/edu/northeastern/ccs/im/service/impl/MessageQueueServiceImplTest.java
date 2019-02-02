package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import edu.northeastern.ccs.im.model.MessageQueue;
import edu.northeastern.ccs.im.model.MessageType;
import edu.northeastern.ccs.im.model.SingleMessage;
import edu.northeastern.ccs.im.service.MessageQueueService;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.*;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

class MessageQueueServiceImplTest {

    private static MessageQueueService messageQueueService;
    private static MongoServer server;
    private static MongoClient client;

    private static MongoCollection<MessageQueue> collection;

    @BeforeAll
    public static void setUp() throws Exception {
        messageQueueService = new MessageQueueServiceImpl();
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        client = new MongoClient(new ServerAddress(serverAddress));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        collection = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("messageQueue", MessageQueue.class);
        Field col = MessageQueueServiceImpl.class.getDeclaredField("collection");
        col.setAccessible(true);
        col.set(messageQueueService, collection);
    }

    @Test
    void addToQueue() {
        Map<String, List<SingleMessage>> queue = new HashMap<>();
        queue.put("VV", new LinkedList<>());
        SingleMessage message = new SingleMessage("Hi, How are you?", MessageType.TEXT);
        MessageQueue queue1 = messageQueueService.addToQueue("VV", "AA", message);
        assertEquals("AA", queue1.getReceiverName());
        assertEquals(1, queue1.getQueue().get("VV").size());
        assertEquals(1, queue1.getQueue().size());

        queue1 = messageQueueService.addToQueue("VV", "AA", message);
        assertEquals(2, queue1.getQueue().get("VV").size());
        assertEquals(1, queue1.getQueue().size());

        queue1 = messageQueueService.addToQueue("RR", "AA", message);
        assertEquals(1, queue1.getQueue().get("RR").size());
        assertEquals(2, queue1.getQueue().size());
    }

    @Test
    void searchByReceiver() {
        MessageQueue queue1 = messageQueueService.searchByReceiver("AA");
        assertNull(queue1);
    }

    @Test
    void getAndRemoveQueue() {
        SingleMessage message = new SingleMessage("ABC", MessageType.TEXT);
        Map<String, List<SingleMessage>> queue = new HashMap<>();
        messageQueueService.addToQueue("VVV", "AAA", message);
        messageQueueService.addToQueue("VVV", "AAA", message);
        messageQueueService.addToQueue("VVV", "AAA", message);
        List<SingleMessage> list = messageQueueService.getAndRemoveQueue("VVV", "AAA");
        assertEquals(3, list.size());
        list = messageQueueService.getAndRemoveQueue("VVV", "AAA");
        assertEquals(0, list.size());
    }
}