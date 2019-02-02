package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.northeastern.ccs.im.model.Subpoena;
import edu.northeastern.ccs.im.server.Prattle;
import edu.northeastern.ccs.im.service.SubpeonaServices;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class SubpeonaServicesImpl implements SubpeonaServices {

    private static final String DATABASE_CONN_STRING = Prattle.getConnectionString();
    private static final MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Subpoena> collection;
    private static final CodecRegistry pojoCodecRegistry;
    private static final String SECRET_KEY = "varadrutvasaurab";

    static {
        MongoClientURI connectionString = new MongoClientURI(DATABASE_CONN_STRING);
        mongoClient = new MongoClient(connectionString);
        database = mongoClient.getDatabase(Prattle.getDatabaseName());
        pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        database = database.withCodecRegistry(pojoCodecRegistry);
        collection = database.getCollection("subpoena", Subpoena.class);
    }

    @Override
    public Subpoena createSubpoena(String username, String pwd, String user1, String user2, String groupName) {
        Subpoena subpoena = findByName(username);
        if (subpoena != null) {
            return null;
        } else {
            collection.insertOne(new Subpoena(username, pwd, user1, user2, groupName, new Date(), new Date()));
        }
        return findByName(username);
    }

    @Override
    public Subpoena findByName(String username) {
        return collection.find(eq("username", username)).first();
    }

    @Override
    public List<Subpoena> findByUsersMonitored(String user1, String user2) {
        return addToList(collection.find(and(eq("user1", user1), eq("user2", user2))));
    }

    @Override
    public List<Subpoena> findByGroupMonitored(String groupName) {
        return addToList(collection.find(eq("groupName", groupName)));
    }

    @Override
    public Subpoena findSubpeona(String username, String password) {
        return collection.find(and(eq("username", username), eq("password", encryptPassword(password)))).first();
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

    /**
     * Creates and returns a list from FindIterable
     * @param iterable      Given FindIterable
     */
    private List<Subpoena> addToList(FindIterable<Subpoena> iterable) {
        List<Subpoena> subpoenaList = new LinkedList<>();
        for (Subpoena subpoena : iterable) {
            subpoenaList.add(subpoena);
        }
        return subpoenaList;
    }
}
