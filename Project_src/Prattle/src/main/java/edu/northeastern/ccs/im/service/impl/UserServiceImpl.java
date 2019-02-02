package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import edu.northeastern.ccs.im.exception.UserAlreadyExistsException;
import edu.northeastern.ccs.im.model.User;
import edu.northeastern.ccs.im.model.UserDTO;
import edu.northeastern.ccs.im.server.Prattle;
import edu.northeastern.ccs.im.service.UserService;
import com.mongodb.client.MongoCollection;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.security.Key;
import java.util.Base64;

import static com.mongodb.client.model.Filters.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Implements the CRUD operations on user as defined in UserService interface
 * @author rohanchitnis
 */
public class UserServiceImpl implements UserService {

    private static final String DATABASE_CONN_STRING = Prattle.getConnectionString();
    private static final MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<User> collection;
    private static final CodecRegistry pojoCodecRegistry;
    private static final String SECRET_KEY = "varadrutvasaurab";

    static {
        MongoClientURI connectionString = new MongoClientURI(DATABASE_CONN_STRING);
        mongoClient = new MongoClient(connectionString);
        database = mongoClient.getDatabase(Prattle.getDatabaseName());
        pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        database = database.withCodecRegistry(pojoCodecRegistry);
        collection = database.getCollection("userDB", User.class);
    }

    /**
     * Encrypts given password using AES algorithm
     * @param withoutEncryption     Plain password
     * @return                      Encrypted password
     */
    private String encryptPassword(String withoutEncryption) {
        try {
            Key key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encoded = cipher.doFinal(withoutEncryption.getBytes());
            return new String(Base64.getEncoder().encode(encoded));
        }catch (Exception exc) { return null; }
    }

    @Override
    public User createUser(UserDTO userDTO) throws UserAlreadyExistsException {
        User user = findUserWithUserName(userDTO.getName());
        if (user != null) {
            throw new UserAlreadyExistsException(user.getName() + " already exists!");
        }
        User newUser = new User(
                userDTO.getName(),
                userDTO.getEmail(),
                userDTO.getPhone(),
                encryptPassword(userDTO.getPassword()),
                "168.1.1.90"
        );
        collection.insertOne(newUser);
        return newUser;
    }

    @Override
    public User updateUser(UserDTO oldUser, UserDTO newUser) {
        User user = findUserWithCredentials(oldUser.getName(), oldUser.getPassword());
        if(user == null) return null;
        user.setEmail(newUser.getEmail());
        user.setName(newUser.getName());
        user.setPassword(encryptPassword(newUser.getPassword()));
        user.setPhone(newUser.getPhone());
        collection.replaceOne(eq("name", oldUser.getName()), user);
        return user;
    }

    @Override
    public User deleteUser(String userName) {
        User user = findUserWithUserName(userName);
        if(user == null) return null;
        collection.deleteOne(eq("name", userName));
        return user;
    }

    @Override
    public User findUserWithUserName(String username) {
        return collection.find(eq("name", username)).first();
    }

    @Override
    public User findUserWithCredentials(String username, String password) {
        return collection.find(and(eq("name", username), eq("password", encryptPassword(password))))
                .first();
    }

    @Override
    public User updateUser(User oldUser, User newUser) {
        User user = findUserWithUserName(oldUser.getName());
        if (user == null) return null;
        collection.replaceOne(eq("name", oldUser.getName()), newUser);
        return findUserWithUserName(newUser.getName());
    }

    @Override
    public User findById(ObjectId id) {
        return collection.find(eq("_id", id)).first();
    }

    @Override
    public User updateIP(String username, String ip) {
        User user = findUserWithUserName(username);
        user.setIp(ip);
        updateUser(user, user);
        return findUserWithUserName(username);
    }
}
