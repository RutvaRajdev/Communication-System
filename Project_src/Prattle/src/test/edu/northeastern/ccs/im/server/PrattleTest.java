package edu.northeastern.ccs.im.server;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.ParentServerTest;
import edu.northeastern.ccs.im.SocketNB;
import edu.northeastern.ccs.im.model.MessageType;
import edu.northeastern.ccs.im.model.SingleMessage;
import edu.northeastern.ccs.im.model.Subpoena;
import edu.northeastern.ccs.im.service.MessageQueueService;
import edu.northeastern.ccs.im.service.SubpeonaServices;
import edu.northeastern.ccs.im.service.impl.MessageQueueServiceImpl;
import edu.northeastern.ccs.im.service.impl.SubpeonaServicesImpl;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrattleTest extends ParentServerTest {

    @Test
    public void broadcastMessage() {
        Prattle.broadcastMessage(Message.makeBroadcastMessage("ABC", "XYZ"));
    }

    @Test
    public void shouldEnqueueMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr,true);
        Prattle.addToActiveThread(cr);
        Prattle.broadcastMessage(Message.makeBroadcastMessage("VARAD","VARAD"));
        assertEquals(1, cr.getWaitingQueue().size());
    }

    @Test
    public void shouldSendOneToOneMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr, true);
        Field field1 = ClientRunnable.class.getDeclaredField("name");
        field1.setAccessible(true);
        field1.set(cr, "VARAD");
        Prattle.addToActiveThread(cr);
        Prattle.sendOneToOne(Message.makeBroadcastMessage("VARAD", "VARAD"), "VARAD");
        assertEquals(1, cr.getWaitingQueue().size());
    }

    @Test
    public void spawnNewThread() throws Exception {
        Method method = Prattle.class.getDeclaredMethod("spawnNewThread", ServerSocketChannel.class, ScheduledExecutorService.class);
        method.setAccessible(true);
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        ConcurrentLinkedQueue<ClientRunnable> queue = new ConcurrentLinkedQueue<>();
        Field field = Prattle.class.getDeclaredField("active");
        field.setAccessible(true);
        field.set(null, queue);
        method.invoke(null, serverSocket, Executors.newScheduledThreadPool(10));
        assertEquals(0, queue.size());
    }

    @Test
    public void shouldSendOneToGroupeMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr, true);
        Field field1 = ClientRunnable.class.getDeclaredField("name");
        field1.setAccessible(true);
        field1.set(cr, "VARAD");
        Prattle.addToActiveThread(cr);
        Prattle.sendToGroup(Message.makeBroadcastMessage("VARAD", "VARAD"), "VARAD", "GRP");
        assertEquals(1, cr.getWaitingQueue().size());
    }

    @Test
    public void shouldreplaceLastMessageWithRecall() throws Exception {
        MessageQueueServiceImpl messageQueueService = mock(MessageQueueServiceImpl.class);
        Field field = Prattle.class.getDeclaredField("messageQueueService");
        field.setAccessible(true);
        field.set(null, messageQueueService);
        List<SingleMessage> toReturn = new ArrayList<>();
        toReturn.add(new SingleMessage("VARAD", MessageType.TEXT));
        toReturn.add(new SingleMessage("VARAD", MessageType.TEXT));
        when(messageQueueService.getAndRemoveQueue("VV", "AA")).thenReturn(toReturn);
        Prattle.replaceLastMessageWithRecall(Message.makePersonalMessage("VV","DATA"),"AA");
        assertEquals(1, messageQueueService.getAndRemoveQueue("VV", "AA").size());
    }

    @Test
    public void shouldreplaceLastMessageWithRecallForGroup() throws Exception {
        MessageQueueServiceImpl messageQueueService = mock(MessageQueueServiceImpl.class);
        Field field = Prattle.class.getDeclaredField("messageQueueService");
        field.setAccessible(true);
        field.set(null, messageQueueService);
        List<SingleMessage> toReturn = new ArrayList<>();
        toReturn.add(new SingleMessage("RECALL", MessageType.TEXT));
        toReturn.add(new SingleMessage("VARAD", MessageType.TEXT));
        when(messageQueueService.getAndRemoveQueue("GRP", "AA")).thenReturn(toReturn);
        Prattle.replaceLastMessageWithRecallForGroup("AA", "GRP");
        assertEquals(1, messageQueueService.getAndRemoveQueue("GRP", "AA").size());
    }

    @Test
    void sendUserMessageToSubpoena() throws Exception {
        SubpeonaServices subpeonaServices = new SubpeonaServicesImpl();
        MongoServer server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        MongoClient client = new MongoClient(new ServerAddress(serverAddress));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoCollection<Subpoena> collection = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("subpoena", Subpoena.class);
        Field col = SubpeonaServicesImpl.class.getDeclaredField("collection");
        col.setAccessible(true);
        col.set(subpeonaServices, collection);
        subpeonaServices.createSubpoena("u", "pwd","u1", "u2", null);
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr, true);
        Prattle.addToActiveThread(cr);
        Method method = Prattle.class.getDeclaredMethod("sendUserMessageToSubpoena", Message.class, String.class);
        method.setAccessible(true);
        ConcurrentLinkedQueue<ClientRunnable> queue = new ConcurrentLinkedQueue<>();
        Field field1 = Prattle.class.getDeclaredField("active");
        field1.setAccessible(true);
        queue.add(cr);
        field1.set(null, queue);
        method.invoke(null, Message.makePersonalMessage("u1", "ABC"), "u2");
        assertEquals(1, queue.size());
    }

    @Test
    void sendGroupMessageToSubpoena() throws Exception {
        SubpeonaServices subpeonaServices = new SubpeonaServicesImpl();
        MongoServer server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        MongoClient client = new MongoClient(new ServerAddress(serverAddress));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoCollection<Subpoena> collection = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("subpoena", Subpoena.class);
        Field col = SubpeonaServicesImpl.class.getDeclaredField("collection");
        col.setAccessible(true);
        col.set(subpeonaServices, collection);
        subpeonaServices.createSubpoena("u", "pwd",null, null, "grp");
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr, true);
        Prattle.addToActiveThread(cr);
        Method method = Prattle.class.getDeclaredMethod("sendGroupMessageToSubpoena", Message.class, String.class);
        method.setAccessible(true);
        ConcurrentLinkedQueue<ClientRunnable> queue = new ConcurrentLinkedQueue<>();
        Field field1 = Prattle.class.getDeclaredField("active");
        field1.setAccessible(true);
        queue.add(cr);
        field1.set(null, queue);
        method.invoke(null, Message.makePersonalMessage("u1", "ABC"), "grp");
        assertEquals(1, queue.size());
    }
}