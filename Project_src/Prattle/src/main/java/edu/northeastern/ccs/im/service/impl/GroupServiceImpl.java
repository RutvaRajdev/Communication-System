package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.northeastern.ccs.im.exception.GroupAlreadyExistsException;
import edu.northeastern.ccs.im.model.ChatGroup;
import static com.mongodb.client.model.Filters.*;

import edu.northeastern.ccs.im.model.User;
import edu.northeastern.ccs.im.server.Prattle;
import edu.northeastern.ccs.im.service.GroupService;
import edu.northeastern.ccs.im.service.UserService;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Implements CRUD operations on ChatGroup as specified in the GroupService interface
 * @author rohanchitnis
 */
public class GroupServiceImpl implements GroupService {

    private static final String DATABASE_CONN_STRING = Prattle.getConnectionString();
    private static final MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<ChatGroup> collection;
    private static final CodecRegistry pojoCodecRegistry;
    private static UserService userService;

    static {
        userService = new UserServiceImpl();
        MongoClientURI connectionString = new MongoClientURI(DATABASE_CONN_STRING);
        mongoClient = new MongoClient(connectionString);
        database = mongoClient.getDatabase(Prattle.getDatabaseName());
        pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        database = database.withCodecRegistry(pojoCodecRegistry);
        collection = database.getCollection("group", ChatGroup.class);
    }

    @Override
    public ChatGroup createGroup(String username, String groupName) throws GroupAlreadyExistsException {
        ChatGroup group = findGroupWithGroupName(groupName);
        if (group != null) {
            throw new GroupAlreadyExistsException("Group with name - " + groupName + " already exists!");
        }
        User user = userService.findUserWithUserName(username);
        ChatGroup chatGroup = new ChatGroup(groupName, user.getId());
        collection.insertOne(chatGroup);
        chatGroup = findGroupWithGroupName(groupName);
        user.getGroupIds().add(chatGroup.getId());
        userService.updateUser(user, user);
        return chatGroup;
    }

    @Override
    public ChatGroup findGroupWithGroupName(String groupName) {
        return collection.find(eq("name", groupName)).first();
    }

    @Override
    public ChatGroup updateGroup(String oldName, String newName) {
        ChatGroup grp = findGroupWithGroupName(oldName);
        if (grp == null) return null;
        grp.setName(newName);
        collection.replaceOne(eq("name", oldName), grp);
        return grp;
    }

    @Override
    public ChatGroup deleteGroup(String groupName) {
        ChatGroup grp = findGroupWithGroupName(groupName);
        if (grp == null) return null;
        collection.deleteOne(eq("name", groupName));
        return grp;
    }

    @Override
    public ChatGroup addUserToGroup(ChatGroup group, String username) {
        User user = userService.findUserWithUserName(username);
        if (user == null) return null;
        group.getMembers().add(user.getId());
        collection.findOneAndReplace(eq("name", group.getName()), group);
        user.getGroupIds().add(group.getId());
        userService.updateUser(user, user);
        return findGroupWithGroupName(group.getName());
    }

    @Override
    public ChatGroup removeUserFromGroup(ChatGroup group, String username) {
        User user = userService.findUserWithUserName(username);
        if (user == null) return findGroupWithGroupName(group.getName());
        group.getMembers().remove(user.getId());
        collection.findOneAndReplace(eq("name", group.getName()), group);
        user.getGroupIds().remove(group.getId());
        userService.updateUser(user, user);
        return findGroupWithGroupName(group.getName());
    }
}
