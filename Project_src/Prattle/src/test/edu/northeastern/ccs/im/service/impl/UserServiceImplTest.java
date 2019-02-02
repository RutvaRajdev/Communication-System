package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import edu.northeastern.ccs.im.exception.UserAlreadyExistsException;
import edu.northeastern.ccs.im.model.User;
import edu.northeastern.ccs.im.model.UserDTO;
import edu.northeastern.ccs.im.service.UserService;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceImplTest {

    private static UserService userService;
    private static MongoServer server;
    private static MongoClient client;

    private static MongoCollection<User> collection;

    @BeforeAll
    public static void setUp() throws Exception {
        userService = new UserServiceImpl();
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
        col.set(userService, collection);
    }

    @Test
    void createUser() throws Exception {
        String uuid = UUID.randomUUID().toString();
        UserDTO user = new UserDTO("team-103"+uuid, "team-103@husky.neu.edu", "8675657777", "1234");
        userService.createUser(user);
        try {
            userService.createUser(user);
        } catch (Exception exc) {
            assertTrue(userService.findUserWithUserName("team-103"+uuid) != null);
            assertTrue(userService.findUserWithCredentials("team-103"+uuid, "1234") != null);
            assertTrue(exc instanceof UserAlreadyExistsException);
        }
    }

    @Test
    public void deleteAndUpdateUser() throws Exception {
        String uuid = UUID.randomUUID().toString();

        //Creating user
        UserDTO user = new UserDTO("team-103"+uuid, "team-103@husky.neu.edu", "8675657777", "1234");
        userService.createUser(user);
        try {
            userService.createUser(user);
        } catch (Exception exc) {
            assertTrue(exc instanceof UserAlreadyExistsException);
        }
        // Delete user
        assertTrue(userService.deleteUser("team-103"+uuid) instanceof User);
        assertNull(userService.deleteUser("team-103"+uuid));

        // Updating user
        user = new UserDTO("team-103"+uuid, "team-103@husky.neu.edu", "8675657777", "1234");
        userService.createUser(user);
        UserDTO newUserDTO = new UserDTO("team-104"+uuid,"team-103@husky.neu.edu","8675657777","12345");
        userService.updateUser(user, newUserDTO);
        //User updatedUser = collection.find(eq("name", "team-104"+uuid)).first();
        User updatedUser = userService.findUserWithUserName("team-104"+uuid);
        assertEquals("team-104"+uuid, updatedUser.getName());
        assertEquals("D7Q3DWpneElAdopYPaSxBQ==", updatedUser.getPassword());

        // Delete user
        User deletedUser = userService.deleteUser("team-104"+uuid);
        assertTrue(deletedUser instanceof User);
        assertTrue(deletedUser.getName().equals("team-104"+uuid));
    }

    @Test
    void udpateUser_checkNULL() {
        String uuid = UUID.randomUUID().toString();
        UserDTO oldUser = new UserDTO("team-103"+uuid, "team-103@husky.neu.edu", "8675657777", "1234");
        UserDTO userDTO = new UserDTO("team-104","team-103@husky.neu.edu","8675657777","12345");
        assertTrue(userService.updateUser(oldUser, userDTO)==null);
    }

    @Test
    void deleteUser_checkNULL() {
        assertTrue(userService.deleteUser("team-104")==null);
    }

    @Test
    public void testUpdateUser() throws Exception {
        User user1 = new User("old", "o", "o", "o", "1.1.1.1");
        User user2 = new User("new", "n", "n", "n", "1.1.1.1");
        ObjectId o1 = new ObjectId("107f1f77bcf86cd799439011");
        ObjectId o2 = new ObjectId("207f1f77bcf86cd799439011");
        ObjectId o3 = new ObjectId("307f1f77bcf86cd799439011");
        Set<ObjectId> objects = new HashSet<>();
        objects.add(o1);
        objects.add(o2);
        user1.setGroupIds(objects);
        objects.add(o3);
        user2.setGroupIds(objects);
        user2.setId(null);
        userService.createUser(new UserDTO("old", "o", "o", "o"));
        User user = userService.updateUser(userService.findUserWithUserName("old"), user2);
        assertEquals(3, user.getGroupIds().size());
        assertEquals("new", user.getName());

        // check for null
        user1 = new User("old1", "o", "o", "o", "1.1.1.1");
        assertNull(userService.updateUser(user1, user2));
    }

    @Test
    void findById() throws Exception {
        String uuid = UUID.randomUUID().toString();
        UserDTO user = new UserDTO("team-103"+uuid, "team-103@husky.neu.edu", "8675657777", "1234");
        userService.createUser(user);
        User user1 = userService.findUserWithUserName("team-103"+uuid);
        User user2 = userService.findById(user1.getId());
        assertEquals("8675657777", user2.getPhone());
    }

    @Test
    void updateIp() throws Exception {
        String uuid = UUID.randomUUID().toString();
        UserDTO user = new UserDTO("team-103"+uuid, "team-103@husky.neu.edu", "8675657777", "1234");
        userService.createUser(user);
        User user2 = userService.updateIP("team-103"+uuid, "1.1.1.1");
        assertEquals("1.1.1.1", user2.getIp());
        user2 = userService.updateIP("team-103"+uuid, "168.10.10.199");
        assertEquals("168.10.10.199", user2.getIp());
    }

    @AfterAll
    public static void cleanUp() {
        client.close();
        server.shutdownNow();
    }
}