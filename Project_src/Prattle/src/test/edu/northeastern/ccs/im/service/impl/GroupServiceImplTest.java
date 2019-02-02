package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import edu.northeastern.ccs.im.exception.GroupAlreadyExistsException;
import edu.northeastern.ccs.im.model.ChatGroup;
import edu.northeastern.ccs.im.model.User;
import edu.northeastern.ccs.im.model.UserDTO;
import edu.northeastern.ccs.im.service.GroupService;
import edu.northeastern.ccs.im.service.UserService;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

class GroupServiceImplTest {

    private static GroupService groupService;
    private static UserService userService;
    private static MongoServer server;
    private static MongoClient client;
    private static MongoCollection<ChatGroup> collection;
    private static MongoCollection<User> collection2;

    @BeforeAll
    public static void setUp() throws Exception {
        groupService = new GroupServiceImpl();
        userService = new UserServiceImpl();
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        client = new MongoClient(new ServerAddress(serverAddress));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        collection = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("group", ChatGroup.class);
        collection2 = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("group", User.class);
        Field col = GroupServiceImpl.class.getDeclaredField("collection");
        col.setAccessible(true);
        col.set(groupService, collection);
        Field col2 = UserServiceImpl.class.getDeclaredField("collection");
        col2.setAccessible(true);
        col2.set(userService, collection2);
    }

    @Test
    void createGroup() throws Exception {
        String uuid = UUID.randomUUID().toString();
        userService.createUser(new UserDTO("student111"+uuid, "", "", "1"));
        assertEquals("friends"+uuid, groupService.createGroup("student111"+uuid, "friends"+uuid).getName());
        try {
            groupService.createGroup("student111"+uuid, "friends"+uuid);
        } catch (Exception exc) {
            assertTrue(exc instanceof GroupAlreadyExistsException);
        }
        assertNotNull(groupService.findGroupWithGroupName("friends"+uuid));
        assertEquals("friends"+uuid, groupService.findGroupWithGroupName("friends"+uuid).getName());
        assertNull(groupService.findGroupWithGroupName("friends1"+uuid));
        assertNotNull(new ArrayList<>(
                groupService.findGroupWithGroupName("friends"+uuid).getMembers()).get(0));
    }

    @Test
    void updateGroup() throws Exception {
        String uuid = UUID.randomUUID().toString();
        userService.createUser(new UserDTO("student1112", "","","123"));
        groupService.createGroup("student1112", "friendsOld_"+uuid);
        groupService.updateGroup("friendsOld_"+uuid, "friendsNew_"+uuid);
        assertEquals("friendsNew_"+uuid, groupService.findGroupWithGroupName("friendsNew_"+uuid).getName());
    }

    @Test
    void updateGroupNotExistent() {
        assertNull(groupService.updateGroup("friends28762387", "7562856"));
    }

    @Test
    void deleteGroup() throws Exception {
        userService.createUser(new UserDTO("student111", "","","123"));
        groupService.createGroup("student111", "friends123");
        assertNotNull(groupService.findGroupWithGroupName("friends123"));
        groupService.deleteGroup("friends123");
        assertNull(groupService.deleteGroup("friends123"));
    }

    @Test
    public void addRemoveUserToGroup() throws Exception {
        userService.createUser(new UserDTO("student1145", "","","123"));
        userService.createUser(new UserDTO("student1144", "","","123"));
        ChatGroup group = groupService.createGroup("student1144", "friends1234");
        assertEquals(1, group.getMembers().size());
        group = groupService.findGroupWithGroupName("friends1234");
        groupService.addUserToGroup(group, "student1145");
        group = groupService.findGroupWithGroupName("friends1234");
        assertEquals(2, group.getMembers().size());
        assertNull(groupService.addUserToGroup(group, "student1146"));
        assertEquals(2, group.getMembers().size());
        assertEquals(1, userService.findUserWithUserName("student1144").getGroupIds().size());
        assertEquals(1, userService.findUserWithUserName("student1145").getGroupIds().size());
        userService.createUser(new UserDTO("student11448", "","","123"));
        group = groupService.createGroup("student11448", "friends12345");
        assertEquals(1, userService.findUserWithUserName("student11448").getGroupIds().size());
        groupService.addUserToGroup(group, "student11448");
        assertEquals(1, userService.findUserWithUserName("student11448").getGroupIds().size());
        group = groupService.findGroupWithGroupName("friends1234");
        groupService.addUserToGroup(group, "student11448");
        assertEquals(2, userService.findUserWithUserName("student11448").getGroupIds().size());
        assertEquals(3, group.getMembers().size());

        // removing existing user
        group = groupService.removeUserFromGroup(groupService.findGroupWithGroupName("friends1234"), "student11448");
        assertEquals(1, userService.findUserWithUserName("student11448").getGroupIds().size());
        assertEquals(2, group.getMembers().size());

        // removing non existing user
        group = groupService.removeUserFromGroup(groupService.findGroupWithGroupName("friends1234"), "1student11448");
        assertEquals(1, userService.findUserWithUserName("student11448").getGroupIds().size());
        assertEquals(2, group.getMembers().size());
    }

    @AfterAll
    public static void cleanUp() {
        client.close();
        server.shutdownNow();
    }

}