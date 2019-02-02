package edu.northeastern.ccs.im.server;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.ParentServerTest;
import edu.northeastern.ccs.im.ScanNetNB;
import edu.northeastern.ccs.im.model.*;
import edu.northeastern.ccs.im.service.GroupService;
import edu.northeastern.ccs.im.service.MessageService;
import edu.northeastern.ccs.im.service.UserService;
import edu.northeastern.ccs.im.service.impl.GroupServiceImpl;
import edu.northeastern.ccs.im.service.impl.MessageServiceImpl;
import edu.northeastern.ccs.im.service.impl.UserServiceImpl;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

public class ClientRunnableTest extends ParentServerTest {

    private ClientRunnable clientRunnable;
    private static UserService us;
    private static GroupService groupService;
    private static MessageService messageService;
    private static MongoServer server;
    private static MongoClient client;

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
        us = new UserServiceImpl();
        groupService = new GroupServiceImpl();
        messageService = new MessageServiceImpl();
        MongoCollection<User> collection;
        MongoCollection<ChatGroup> collection1;
        MongoCollection<ChatMessage> collection2;
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        client = new MongoClient(new ServerAddress(serverAddress));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        collection = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("user", User.class);
        Field col = UserServiceImpl.class.getDeclaredField("collection");
        col.setAccessible(true);
        col.set(us, collection);
        collection1 = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("group", ChatGroup.class);
        Field col1 = GroupServiceImpl.class.getDeclaredField("collection");
        col1.setAccessible(true);
        col1.set(groupService, collection1);

        collection2 = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("message", ChatMessage.class);
        Field col2 = MessageServiceImpl.class.getDeclaredField("collection");
        col2.setAccessible(true);
        col2.set(messageService, collection2);
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        clientRunnable = new ClientRunnable(getSocketChannel());
    }

    @Test
    public void getName() {
        clientRunnable.setName("ABC");
        assertEquals("ABC", clientRunnable.getName());
    }

    @Test
    public void setName() {
        clientRunnable.setName("XYZ");
        assertEquals("XYZ", clientRunnable.getName());
    }

    @Test
    public void getUserId() {
        assertEquals(0, clientRunnable.getUserId());
    }

    @Test
    public void isInitialized() {
        assertFalse(clientRunnable.isInitialized());
    }

    @Test
    public void testTerminateClient() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        clientRunnable = new ClientRunnable(channel);
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(10);
        ScheduledFuture clientFuture = threadPool.scheduleAtFixedRate(clientRunnable, 10,
                10, TimeUnit.MILLISECONDS);
        clientRunnable.setFuture(clientFuture);
        clientRunnable.terminateClient();
        assertFalse(channel.isOpen());
    }

    @Test
    public void run() throws Exception {
        ScanNetNB input = Mockito.mock(ScanNetNB.class);
        when(input.hasNextMessage()).thenReturn(true);
        MockitoAnnotations.initMocks(clientRunnable);
        Field initialized = ClientRunnable.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.set(clientRunnable, true);
        clientRunnable.run();
        assertTrue(clientRunnable.isInitialized());
    }

    @Test
    public void shouldAddDeleteMessageToQueue() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        us.createUser(new UserDTO(username,"","",password));
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        method.invoke(cr, Message.makeDeleteMessage(username));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 1);
        us.deleteUser(username);
    }

    @Test
    public void shouldAddUpdateMessageToQueue() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        us.createUser(new UserDTO(username,"","",password));
        cr.setPass(password);
        method.invoke(cr, Message.makeUpdateUsernameMessage(username,password));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 1);
        us.deleteUser(username);
    }

    @Test
    public void shouldAddUpdatePasswordMessageToQueue() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        us.createUser(new UserDTO(username,"","",password));
        cr.setPass(password);
        method.invoke(cr, Message.makeUpdatePasswordMessage(username,password));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 1);
        us.deleteUser(username);
    }

    @Test
    public void shouldCallImmediateResponse() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        cr.addImmediateResponse(Message.makeQuitMessage("varad"));
        Method method = ClientRunnable.class.getDeclaredMethod("performImmediateResponseIfRequired");
        method.setAccessible(true);
        method.invoke(cr);
        Queue<Message> waitingList = cr.getImmediateResponseQueue();
        assertEquals(waitingList.size(), 0);
    }

    @Test
    public void shouldHandleSpecialResponse() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("handleSpecial", Message.class);
        method.setAccessible(true);
        method.invoke(cr, Message.makeQuitMessage("VARAD"));
        Queue<Message> waitingList = cr.getSpecialResponseQueue();
        assertEquals(waitingList.size(), 1);
    }

    @Test
    public void shouldBroadcastIsSpecialMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("broadcastMessageIsSpecial", Message.class, List.class);
        method.setAccessible(true);
        List<Message> messages = new ArrayList<>();
        messages.add(Message.makeBroadcastMessage("VARAD", "VARAD"));
        Object value = method.invoke(cr, Message.makeBroadcastMessage("VARAD","VARAD"), messages);
        boolean returned = (Boolean) value;
        assertEquals(returned, true);
    }

    @Test
    public void shouldSendLoginSuccessfulMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("sendAcknowledgement", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr,true);
        us.createUser(new UserDTO("VARAD","","","VARAD"));
        method.invoke(cr, Message.makeSimpleLoginMessage("VARAD","PASSWORD--VARAD IP--127.0.0.1:5555"));
        String whichMessage = cr.getWhichMessageSent();
        assertEquals(whichMessage, "LOGIN_SUCCESSFUL");
    }

    @Test
    public void shouldSendLoginFailedMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("sendAcknowledgement", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr,false);
        method.invoke(cr, Message.makeSimpleLoginMessage("VARAD","PASSWORD--VARAD IP--127.0.0.1:5555"));
        String whichMessage = cr.getWhichMessageSent();
        assertEquals(whichMessage, "LOGIN_FAILED");
    }

    @Test
    public void shouldReturnFalseAsNoUsername() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("setUserName", String.class);
        method.setAccessible(true);
        String msg = null;
        Object value = method.invoke(cr, msg);
        boolean returned = (Boolean) value;
        assertEquals(returned, false);
    }

    @Test
    public void shouldReturnTrueAsNoUsername() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("setUserName", String.class);
        method.setAccessible(true);
        String msg = "VARAD";
        Object value = method.invoke(cr, msg);
        boolean returned = (Boolean) value;
        assertEquals(returned, true);
    }

    @Test
    public void shouldBeAProperMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("messageChecks", Message.class);
        method.setAccessible(true);
        cr.setName("VARAD");
        Message msg = Message.makeSimpleLoginMessage("VARAD", "VARAD");
        Object value = method.invoke(cr, msg);
        boolean returned = (Boolean) value;
        assertEquals(returned, true);
    }

    @Test
    public void shouldNotBeAProperMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("messageChecks", Message.class);
        method.setAccessible(true);
        cr.setName("VARAD");
        Message msg = null;
        Object value = method.invoke(cr, msg);
        boolean returned = (Boolean) value;
        assertEquals(returned, false);
    }

    @Test
    public void shouldSetInitializationToTrue() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("setInitialization", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        method.invoke(cr, Message.makeRegisterMessage("VARAD","PASSWORD--VARAD IP--127.0.0.1:5555"));
        boolean init = (Boolean) field.get(cr);
        assertEquals(init, true);
    }

    @Test
    public void shouldSetInitializationToFalse() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("setInitialization", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        method.invoke(cr, Message.makeQuitMessage("QUIT"));
        boolean init = (Boolean) field.get(cr);
        assertEquals(init, false);
    }

    @Test
    public void shouldTerminateClient() throws Exception {
        //SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = mock(ClientRunnable.class);
        Method method = ClientRunnable.class.getDeclaredMethod("terminateClientIfTimeout");
        method.setAccessible(true);

        Field terminate = ClientRunnable.class.getDeclaredField("terminate");
        terminate.setAccessible(true);
        terminate.set(cr, false);

        Field shouldTerminate = ClientRunnable.class.getDeclaredField("shouldTerminate");
        shouldTerminate.setAccessible(true);
        shouldTerminate.set(cr, true);
        doNothing().when(cr).terminateClient();
        Object value = method.invoke(cr);
        boolean returned = (Boolean) value;
        assertEquals(returned, true);
    }

    @Test
    public void shouldNotTerminateClient() throws Exception {
        //SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = mock(ClientRunnable.class);
        Method method = ClientRunnable.class.getDeclaredMethod("terminateClientIfTimeout");
        method.setAccessible(true);

        Field terminate = ClientRunnable.class.getDeclaredField("terminate");
        terminate.setAccessible(true);
        terminate.set(cr, true);

        Field shouldTerminate = ClientRunnable.class.getDeclaredField("shouldTerminate");
        shouldTerminate.setAccessible(true);
        shouldTerminate.set(cr, true);
        doNothing().when(cr).terminateClient();
        Object value = method.invoke(cr);
        boolean returned = (Boolean) value;
        assertEquals(returned, false);
    }

    @Test
    public void shouldHaveSetTerminateToTrue() throws Exception {
        //SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = mock(ClientRunnable.class);
        Method method = ClientRunnable.class.getDeclaredMethod("showAllBroadcastedMessages", Message.class);
        method.setAccessible(true);

        Field terminate = ClientRunnable.class.getDeclaredField("terminate");
        terminate.setAccessible(true);


        method.invoke(cr, Message.makeQuitMessage("QUIT"));
        boolean returned = (Boolean) terminate.get(cr);
        assertEquals(returned, true);
    }

    @Test
    public void shouldSetThePassword() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        cr.setPass("123");

        Field terminate = ClientRunnable.class.getDeclaredField("pwd");
        terminate.setAccessible(true);

        String pwd = (String) terminate.get(cr);

        assertEquals(pwd, "123");
    }

    @Test
    public void shouldCheckAndSetPass() throws Exception {
        ClientRunnable cr = mock(ClientRunnable.class);
        Method method = ClientRunnable.class.getDeclaredMethod("setPassword", String.class);
        method.setAccessible(true);
        String pwd = "VARAD";

        Object value = method.invoke(cr, pwd);
        Boolean returned = (Boolean) value;
        assertEquals(returned, true);
    }

    @Test
    public void shouldCheckAndNotSetPassword() throws Exception {
        ClientRunnable cr = mock(ClientRunnable.class);
        Method method = ClientRunnable.class.getDeclaredMethod("setPassword", String.class);
        method.setAccessible(true);
        String pwd = null;

        Object value = method.invoke(cr, pwd);
        Boolean returned = (Boolean) value;
        assertEquals(returned, false);
    }

    @Test
    public void shouldProceedAndSetUsernameAndPassword() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("proceedIfRightCredentials", Message.class);
        method.setAccessible(true);

        method.invoke(cr, Message.makeSimpleLoginMessage("VARAD", "PASSWORD--5656 IP--127.0.0.1:5555"));

        String username = cr.getName();
        Field terminate = ClientRunnable.class.getDeclaredField("pwd");
        terminate.setAccessible(true);

        String password = (String) terminate.get(cr);
        assertEquals(username, "VARAD");
        assertEquals(password, "VV");
    }

    @Test
    public void shouldNotProceedAndSetUsernameAndPassword() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("proceedIfRightCredentials", Message.class);
        method.setAccessible(true);

        method.invoke(cr, Message.makeSimpleLoginMessage(null, null));

        String username = cr.getName();
        Field terminate = ClientRunnable.class.getDeclaredField("pwd");
        terminate.setAccessible(true);

        String password = (String) terminate.get(cr);
        assertEquals(username, "");
        assertEquals(password, null);
    }

    @Test
    public void shouldCheckForInitialization() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("checkForInitialization");
        ScanNetNB scanNetNB = mock(ScanNetNB.class);
        when(scanNetNB.hasNextMessage()).thenReturn(true);
        when(scanNetNB.nextMessage()).thenReturn(Message.makeSimpleLoginMessage("VARAD","VARAD"));
        method.setAccessible(true);

        method.invoke(cr);

        String username = cr.getName();
        assertEquals(username, "");
    }

    @Test
    public void shouldNotCheckForInitAsNoMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("checkForInitialization");
        ScanNetNB scanNetNB = mock(ScanNetNB.class);
        when(scanNetNB.hasNextMessage()).thenReturn(false);
        when(scanNetNB.nextMessage()).thenReturn(Message.makeSimpleLoginMessage("VARAD","VARAD"));
        method.setAccessible(true);

        method.invoke(cr);

        String username = cr.getName();
        assertEquals(username, "");
    }

    @Test
    public void shouldTestForBOMBMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("quitIfBombMessage", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr,true);
        method.invoke(cr, Message.makeSimpleLoginMessage("VARAD", "Prattle says everyone log off"));
        boolean init = (Boolean) field.get(cr);
        assertEquals(init, false);
    }

    @Test
    public void shouldTestForBOMBMessageAndReturTrue() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("quitIfBombMessage", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr,true);
        method.invoke(cr, Message.makeSimpleLoginMessage("VARAD", "VARAD"));
        boolean init = (Boolean) field.get(cr);
        assertEquals(init, true);
    }

    @Test
    public void shouldReturnTrueAsUserDoesNotExist() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("registerUser", String.class, String.class);
        method.setAccessible(true);
        String username = "VARAD"+ UUID.randomUUID().toString();
        String password = "VARAD";
        Object value = method.invoke(cr, username, password);
        boolean init = (Boolean) value;
        assertEquals(init, true);


        Method method2 = ClientRunnable.class.getDeclaredMethod("registerUser", String.class, String.class);
        method2.setAccessible(true);
        Object value2 = method.invoke(cr, username, password);
        boolean init2 = (Boolean) value2;
        assertEquals(init2, false);
    }

    @Test
    public void shouldReturnTrueAsLoginTrue() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("isUserPresent", String.class, String.class);
        method.setAccessible(true);
        String username = "VARAD"+ UUID.randomUUID().toString();
        String password = "VV";
        Object value = method.invoke(cr, username, password);
        boolean init = (Boolean) value;
        assertEquals(init, false);

        Method method3 = ClientRunnable.class.getDeclaredMethod("registerUser", String.class, String.class);
        method3.setAccessible(true);
        method3.invoke(cr, username, password);


        Method method2 = ClientRunnable.class.getDeclaredMethod("isUserPresent", String.class, String.class);
        method2.setAccessible(true);
        Object value2 = method.invoke(cr, username, password);
        boolean init2 = (Boolean) value2;
        assertEquals(init2, true);
    }

    @Test
    public void shouldRegisterSuccessfully() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("sendAcknowledgement", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr,true);
        method.invoke(cr, Message.makeRegisterMessage(UUID.randomUUID().toString(), "PASSWORD--"+UUID.randomUUID().toString()+" IP--127.0.0.1:5555"));
        String whichMessage = cr.getWhichMessageSent();
        assertEquals(whichMessage, "REGISTRATION_SUCCESSFUL");
    }

    @Test
    public void shouldRegisterFail() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("sendAcknowledgement", Message.class);
        method.setAccessible(true);
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(cr,true);
        String username = "VARAD"+ UUID.randomUUID().toString();
        String password = "PASSWORD--VARAD IP--127.0.0.1:5555";
        Method method3 = ClientRunnable.class.getDeclaredMethod("registerUser", String.class, String.class);
        method3.setAccessible(true);
        method3.invoke(cr, username, password);

        method.invoke(cr, Message.makeRegisterMessage(username, password));
        String whichMessage = cr.getWhichMessageSent();
        assertEquals(whichMessage, "REGISTRATION_FAILED");
    }

    @Test
    public void shouldBeTrue() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("showAllBroadcastedMessages", Message.class);
        method.setAccessible(true);
        method.invoke(cr, Message.makeBroadcastMessage("VARAD","Prattle says everyone log off"));
        Field field = ClientRunnable.class.getDeclaredField("initialized");
        field.setAccessible(true);
        boolean init = (Boolean) field.get(cr);
        assertFalse(init);
    }

    @Test
    public void shouldReturnFalseAsThereIsNoSuchUser() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("deleteUser", String.class);
        method.setAccessible(true);
        Object obj = method.invoke(cr, "SOMERANDOMUSERNAME");
        boolean init = (Boolean) obj;
        assertFalse(init);
    }

    @Test
    public void shouldReturnFalseAsThereIsNoSuchPassword() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("updateUsername", String.class, String.class);
        method.setAccessible(true);
        Object obj = method.invoke(cr, "SOMERANDOMUSERNAME", "ANOTHERRANDOMUSERNAME");
        boolean init = (Boolean) obj;
        assertFalse(init);
    }

    @Test
    public void shouldReturnFalseAsThereIsNoSuchUsername() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("updatePassword", String.class, String.class);
        method.setAccessible(true);
        Object obj = method.invoke(cr, "SOMERANDOMUSERNAME", "ANOTHERRANDOMUSERNAME");
        boolean init = (Boolean) obj;
        assertFalse(init);
    }

    @AfterAll
    public static void cleanUp() {
        client.close();
        server.shutdownNow();
    }

    @Test
    public void shouldSetInitToFalse() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("handleCustomMessage", Message.class);
        method.setAccessible(true);
        Field initialized = ClientRunnable.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.set(clientRunnable, true);
        method.invoke(cr, Message.makeSimpleLoginMessage("VARAD", "Prattle says everyone log off"));
        Field initialized1 = ClientRunnable.class.getDeclaredField("initialized");
        initialized1.setAccessible(true);
        assertFalse(cr.isInitialized());
    }

    @Test
    public void shouldHaveCreatedTheNewMessage() {
        try {
            SocketChannel channel = getTempSocketChannel();
            ClientRunnable cr = new ClientRunnable(channel);
            Method method = ClientRunnable.class.getDeclaredMethod("handleCustomMessage", Message.class);
            method.setAccessible(true);
            method.invoke(cr, Message.makePersonalMessage("VARAD", "VV: SOMEMSG"));
            assertTrue(cr.isOneToOne());
        }catch (Exception e){}
    }

    @Test
    public void shouldGetOneToOneFlag() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Field initialized = ClientRunnable.class.getDeclaredField("oneToOne");
        initialized.setAccessible(true);
        initialized.set(cr, true);
        assertTrue(cr.isOneToOne());
    }

    @Test
    public void shouldSendBroadcastMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("handleCustomMessage", Message.class);
        method.setAccessible(true);
        method.invoke(cr, Message.makeBroadcastMessage("VARAD",""));
        assertTrue(cr.isBroadcastFlag());
    }

    @Test
    public void shouldSendGroupMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Set<ObjectId> ids = new HashSet<>();
        ids.add(new ObjectId());
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName("GRP");
        chatGroup.setMembers(ids);
        us.createUser(new UserDTO("VARAD1234","","","1234"));
        groupService.createGroup("VARAD1234","GRP");
        Method method = ClientRunnable.class.getDeclaredMethod("handleCustomMessage", Message.class);
        method.setAccessible(true);
        method.invoke(cr, Message.makeGroupMessage("VARAD1234","GRP:SOMETHING"));
        assertTrue(cr.isToGroup());
    }

    @Test
    public void shouldCreateNewGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeGroupCreationMessage(username, groupName));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldDeleteTheGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeGroupDeletionMessage(username, groupName));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotDeleteGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeGroupDeletionMessage(username, "GG"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldRenameTheGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String newGroupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeGroupRenameMessage(username, groupName+":"+newGroupName));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotRenameTheGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String newGroupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeGroupRenameMessage(username, "RANDOMNAME:"+newGroupName));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldAddUsersToGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeAddUsersToGrpMessage(username, groupName+":VV AA RR"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotAddUsersToGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeAddUsersToGrpMessage(username, "RANDOM:VV AA RR"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldRemoveUsersToGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeRemoveUserFromGrpMessage(username, groupName+":VV AA RR"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotRemoveUsersToGroup() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeRemoveUserFromGrpMessage(username, "RANDOM:VV AA RR"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldDecryptPassword() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("decryptEndToEnd", String.class);
        method.setAccessible(true);
        Object returned = method.invoke(cr,"5656");
        String encoded = (String) returned;
        assertEquals("VV",encoded);
    }

    @Test
    public void shouldReturnNullPassword() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("decryptEndToEnd", String.class);
        method.setAccessible(true);
        Object returned = method.invoke(cr,"@#$%$@#$");
        String encoded = (String) returned;
        assertEquals(null,encoded);
    }

    @Test
    public void shouldGetUserDetails() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeViewUserMessage(username));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotGetUserDetails() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeViewUserMessage("RANDOMUSER"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldSendListOfUsers() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeViewUsersInGroupMessage(username, groupName));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotSendListOfUsers() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeViewUsersInGroupMessage(username, "RANDOMGRP"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotSendBackMessagesListToUSER() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        String username1 = "VARAD1"+UUID.randomUUID().toString();
        String password1 = "VARDOS1"+UUID.randomUUID().toString();
        us.createUser(new UserDTO(username1,"","",password1));
        method.invoke(cr, Message.makeSearchMsgByUNameMessage(username, username1));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldSendBackMessagesListToUSER() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        List<String> al = new ArrayList<>();
        al.add("0.0.0.0");
        messageService.createMessage(username+"$XYZ", "HELLO", MessageType.TEXT, "127.0.0.1", al, username);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeSearchMsgByUNameMessage(username, "XYZ"));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldSendBackGroupMessagesListToUSER() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        List<String> al = new ArrayList<>();
        al.add("0.0.0.0");
        messageService.createMessage(groupName, "HELLO", MessageType.TEXT, "127.0.0.1", al, username);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        method.invoke(cr, Message.makeSearchMsgByGNameMessage(username, groupName));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldSendBackUserMessagesListToUSERByTimestamp() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        List<String> al = new ArrayList<>();
        al.add("0.0.0.0");
        messageService.createMessage(groupName, "HELLO", MessageType.TEXT, "127.0.0.1", al, username);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        String now = new Date().toString();
        String then = new Date().toString();
        method.invoke(cr, Message.makeSearchMsgByTimeStampForUserMessage(username, groupName+", "+now+", "+then));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldSendBackGroupMessagesListToUSERByTimestamp() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        List<String> al = new ArrayList<>();
        al.add("0.0.0.0");
        messageService.createMessage(groupName, "HELLO", MessageType.TEXT, "127.0.0.1", al, username);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        String now = new Date().toString();
        String then = new Date().toString();
        method.invoke(cr, Message.makeSearchMsgByTimeStampForGroupMessage(username, groupName+", "+now+", "+then));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotSendBackGroupMessagesListToUSERByTimestamp() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        List<String> al = new ArrayList<>();
        al.add("0.0.0.0");
        messageService.createMessage(groupName, "HELLO", MessageType.TEXT, "127.0.0.1", al, username);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        String now = "some random thing";
        String then = "another random thing";
        method.invoke(cr, Message.makeSearchMsgByTimeStampForGroupMessage(username, groupName+", "+now+", "+then));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    public void shouldNotSendBackUserMessagesListToUSERByTimestamp() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("listenForCRUDMessages", Message.class);
        method.setAccessible(true);
        String groupName = "GRP"+UUID.randomUUID().toString();
        String username = "VARAD"+UUID.randomUUID().toString();
        String password = "VARDOS"+UUID.randomUUID().toString();
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        us.createUser(new UserDTO(username,"","",password));
        groupService.createGroup(username, groupName);
        List<String> al = new ArrayList<>();
        al.add("0.0.0.0");
        messageService.createMessage(groupName, "HELLO", MessageType.TEXT, "127.0.0.1", al, username);
        Field field = ClientRunnable.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(cr, us);
        Field field1 = ClientRunnable.class.getDeclaredField("groupService");
        field1.setAccessible(true);
        field1.set(cr, groupService);
        String now = "some random thing";
        String then = "another random thing";
        method.invoke(cr, Message.makeSearchMsgByTimeStampForUserMessage(username, groupName+", "+now+", "+then));
        Queue<Message> waitingList = cr.getWaitingQueue();
        assertEquals(waitingList.size(), 0);
        us.deleteUser(username);
    }

    @Test
    void toggleParentalControl() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("toggleParentalControl", Message.class);
        method.setAccessible(true);
        us.createUser(new UserDTO("user987","","","pwd"));
        method.invoke(cr, Message.makePersonalMessage("user987", "ON"));
        assertTrue(us.findUserWithUserName("user987").isParentalControl());
        method.invoke(cr, Message.makePersonalMessage("user987", "msg"));
        assertFalse(us.findUserWithUserName("user987").isParentalControl());
    }

    @Test
    void sendMessageToOneUser() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("sendMessageToOneUser", Message.class);
        method.setAccessible(true);
        us.createUser(new UserDTO("user9876","","","pwd"));
        us.createUser(new UserDTO("AA11","","","pwd"));
        method.invoke(cr, Message.makePersonalMessage("user9876", "AA11:abc"));
        assertTrue(cr.isOneToOne());
    }

    @Test
    void addIPAddressToEachMIMEGroupMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("addIPAddressToEachMIMEGroupMessage",
                Message.class, String.class, String.class);
        method.setAccessible(true);
        us.createUser(new UserDTO("user916","","","pwd"));
        groupService.createGroup("user916", "grp900");
        Message msg = (Message) method.invoke(cr, Message.makePersonalMessage("user916", "AA:abc"),
                "grp900", "abc abc");
        assertTrue(msg.isGroupContainingImageMessage());
    }

    @Test
    void sendChatHistoryToSubpoenaUser() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("sendChatHistoryToSubpoenaUser",
                Message.class);
        method.setAccessible(true);
    }

    @Test
    void filteredMessage() throws Exception {
        SocketChannel channel = getTempSocketChannel();
        ClientRunnable cr = new ClientRunnable(channel);
        Method method = ClientRunnable.class.getDeclaredMethod("filteredMessage", Message.class);
        method.setAccessible(true);
        Message msg = (Message) method.invoke(cr, Message.makePersonalMessage("user916", "AA:abc"));
        assertTrue(msg.isPrivateMessage());
    }
}