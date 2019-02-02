package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.northeastern.ccs.im.model.MessageQueue;
import edu.northeastern.ccs.im.model.SingleMessage;
import edu.northeastern.ccs.im.server.Prattle;
import edu.northeastern.ccs.im.service.MessageQueueService;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * This class provides concrete implementation for MessageQueueService interface
 * to communicate with messageQueue database
 *
 * @author rohanchitnis
 */
public class MessageQueueServiceImpl implements MessageQueueService {

    private static final String DATABASE_CONN_STRING = Prattle.getConnectionString();
    private static final MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<MessageQueue> collection;
    private static final CodecRegistry pojoCodecRegistry;
    private static final String RECEIVER_NAME = "receiverName";

    static {
        MongoClientURI connectionString = new MongoClientURI(DATABASE_CONN_STRING);
        mongoClient = new MongoClient(connectionString);
        database = mongoClient.getDatabase(Prattle.getDatabaseName());
        pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        database = database.withCodecRegistry(pojoCodecRegistry);
        collection = database.getCollection("messageQueue", MessageQueue.class);
    }


    @Override
    public MessageQueue addToQueue(String sender, String receiver, SingleMessage message) {
        MessageQueue queue1 = searchByReceiver(receiver);
        if (queue1 == null) {
            Map<String, List<SingleMessage>> map = new HashMap<>();
            List<SingleMessage> q = new LinkedList<>();
            q.add(message);
            map.put(sender, q);
            MessageQueue queue2 = new MessageQueue(receiver, map);
            collection.insertOne(queue2);
        } else {
            Map<String, List<SingleMessage>> map = queue1.getQueue();
            if (map.containsKey(sender)) {
                List<SingleMessage> q = map.get(sender);
                q.add(message);
                map.remove(sender);
                map.put(sender, q);
            } else {
                List<SingleMessage> q = new LinkedList<>();
                q.add(message);
                map.put(sender, q);
            }
            MessageQueue queue2 = new MessageQueue(receiver, map);
            collection.replaceOne(eq(RECEIVER_NAME, receiver), queue2);
        }
        return searchByReceiver(receiver);
    }

    @Override
    public MessageQueue searchByReceiver(String receiver) {
        return collection.find(eq(RECEIVER_NAME, receiver)).first();
    }

    @Override
    public List<SingleMessage> getAndRemoveQueue(String sender, String receiver) {
        MessageQueue queue1 = searchByReceiver(receiver);
        List<SingleMessage> list = new LinkedList<>();
        if (queue1 != null) {
            Map<String, List<SingleMessage>> map = queue1.getQueue();
            if (map.containsKey(sender)) {
                list = map.get(sender);
            }
            map.remove(sender);
            MessageQueue queue2 = new MessageQueue(receiver, map);
            collection.replaceOne(eq(RECEIVER_NAME, receiver), queue2);
        }
        return list;
    }
}
