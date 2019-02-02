package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import edu.northeastern.ccs.im.model.ChatMessage;
import edu.northeastern.ccs.im.model.MessageType;
import edu.northeastern.ccs.im.model.SingleMessage;
import edu.northeastern.ccs.im.service.MessageService;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

class MessageServiceImplTest {

    private static MongoServer server;
    private static MongoClient client;
    private static MongoCollection<ChatMessage> collection;
    private static MessageService messageService;

    @BeforeAll
    public static void setUp() throws Exception {
        messageService = new MessageServiceImpl();
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        client = new MongoClient(new ServerAddress(serverAddress));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        collection = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("message", ChatMessage.class);
        Field col = MessageServiceImpl.class.getDeclaredField("collection");
        col.setAccessible(true);
        col.set(messageService, collection);
    }

    @Test
    void createMessageForGroup() {
        ChatMessage message = messageService.createMessage("group1", "message", MessageType.EMOJI,
                "", new LinkedList<String>(), "senderName");
        assertNotNull(message);
        message = messageService.createMessage("group1", "message2", MessageType.TEXT,
                "", new LinkedList<String>(), "senderName");
        assertEquals(2, message.getMessages().size());
        assertEquals(MessageType.EMOJI, message.getMessages().get(0).getType());
        assertEquals(MessageType.TEXT, message.getMessages().get(1).getType());
    }

    @Test
    void createMessageForOneToOne() {
        ChatMessage message = messageService.createMessage("rohan$varad", "message",
                MessageType.EMOJI, "", new LinkedList<String>(), "senderName");
        assertNotNull(message);
        assertNull(messageService.findMessage("varad$rohan"));
        assertNotNull(messageService.findMessage("rohan$varad"));
        assertEquals(MessageType.EMOJI, message.getMessages().get(0).getType());
        message = messageService.createMessage("varad$rohan", "message2", MessageType.TEXT,
                "", new LinkedList<String>(), "senderName");
        assertEquals(MessageType.EMOJI, message.getMessages().get(0).getType());
        assertEquals(MessageType.TEXT, message.getMessages().get(1).getType());
        assertEquals("message", message.getMessages().get(0).getMessage());
        assertEquals("message2", message.getMessages().get(1).getMessage());
    }

    @Test
    void testImageMessages() throws Exception {
        byte[] fileContent = FileUtils.readFileToByteArray(new File("src/test/resources/ClassDiagram.jpg"));
        String image = "src/test/resources/ClassDiagram.jpg " + Base64.encodeBase64String(fileContent);
        ChatMessage message = messageService.createMessage("rutva$varad", image, MessageType.IMAGE,
                "", new LinkedList<String>(), "senderName");
        assertNotNull(message);
        ChatMessage message1 = messageService.createMessage("rutva$varad", image, MessageType.VIDEO,
                "", new LinkedList<String>(), "senderName");
        assertNotNull(message1);
    }

    @Test
    void testStoreOnServer() throws Exception {
        Method method = MessageServiceImpl.class.getDeclaredMethod("storeImageOnServer", String.class);
        method.setAccessible(true);
        byte[] fileContent = FileUtils.readFileToByteArray(new File("src/test/resources/ClassDiagram.jpg"));
        assertEquals("jpg",
                FilenameUtils.getExtension((String)method.invoke(messageService,
                        "src/test/resources/ClassDiagram.jpg " + Base64.encodeBase64String(fileContent))));
    }

    @Test
    void findMessage() {
        assertNotNull(messageService.findMessage("group1"));
    }

    @Test
    void searchMessages() {
        messageService.createMessage("rr$vv", "message1", MessageType.EMOJI,
                "", new LinkedList<String>(), "senderName");
        messageService.createMessage("rr$vv", "message2", MessageType.TEXT,
                "", new LinkedList<String>(), "senderName");
        messageService.createMessage("rr$vv", "message3", MessageType.TEXT,
                "", new LinkedList<String>(), "senderName");
        messageService.createMessage("rr$vv", "message4", MessageType.TEXT,
                "", new LinkedList<String>(), "senderName");
        List<SingleMessage> messageList = messageService.searchMessages("rr");
        assertEquals(4, messageList.size());
        messageService.createMessage("vv$rr", "message5", MessageType.TEXT,
                "", new LinkedList<String>(), "senderName");
        messageList = messageService.searchMessages("rr");
        assertEquals(5, messageList.size());
        messageService.createMessage("vv$ss", "message6", MessageType.TEXT,
                "", new LinkedList<String>(), "senderName");
        messageList = messageService.searchMessages("rr");
        assertEquals(5, messageList.size());
    }

    @Test
    void searchMessagesTimestamp() {
        messageService.createMessage("r1$v1", "message1", MessageType.EMOJI,
                "", new LinkedList<String>(), "senderName");
        messageService.createMessage("r1$v1", "message1", MessageType.EMOJI,
                "", new LinkedList<String>(), "senderName");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date date1 = cal.getTime();
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date date2 = cal.getTime();
        assertEquals(2, messageService.searchMessages(date1, date2, "r1$v1").size());
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        date1 = cal.getTime();
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        date2 = cal.getTime();
        assertEquals(0, messageService.searchMessages(date1, date2, "r1$v1").size());
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +2);
        date1 = cal.getTime();
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        date2 = cal.getTime();
        assertEquals(0, messageService.searchMessages(date1, date2, "r1$v1").size());
    }

    @AfterAll
    public static void cleanUp() {
        client.close();
        server.shutdownNow();
    }
}