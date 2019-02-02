package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.northeastern.ccs.im.model.ChatMessage;
import edu.northeastern.ccs.im.model.MessageType;
import edu.northeastern.ccs.im.model.SingleMessage;
import edu.northeastern.ccs.im.server.Prattle;
import edu.northeastern.ccs.im.service.MessageService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Date;
import java.util.logging.Level;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
* Implements Read ans Save operations on message as specified in the MessageService interface
* Messages can be of the type mentioned in MessageType enum
 *
* @author rohanchitnis
*/
public class MessageServiceImpl implements MessageService {

    Logger logger = Logger.getLogger(MessageServiceImpl.class.getName());

    private static final String DATABASE_CONN_STRING = Prattle.getConnectionString();
    private static final MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<ChatMessage> collection;
    private static final CodecRegistry pojoCodecRegistry;

    static {
        MongoClientURI connectionString = new MongoClientURI(DATABASE_CONN_STRING);
        mongoClient = new MongoClient(connectionString);
        database = mongoClient.getDatabase(Prattle.getDatabaseName());
        pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        database = database.withCodecRegistry(pojoCodecRegistry);
        collection = database.getCollection("message", ChatMessage.class);
    }

    @Override
    public ChatMessage createMessage(String groupName, String message, MessageType type, String senderIP,
                                     List<String> receiverIP, String sender) {
        SingleMessage singleMessage;
        String path = "";
        if (isMiMIEType(type)) {
            path = storeImageOnServer(message);
            singleMessage = new SingleMessage("", type);
            singleMessage.setSender(sender);
            singleMessage.setSenderIp(senderIP);
            singleMessage.setReceiverIp(receiverIP);
            singleMessage.setPath(path);
        } else {
            singleMessage = new SingleMessage(message, type);
            singleMessage.setSender(sender);
            singleMessage.setSenderIp(senderIP);
            singleMessage.setReceiverIp(receiverIP);
            singleMessage.setPath(path);
        }
        List<SingleMessage> list = new LinkedList<>();
        list.add(singleMessage);
        ChatMessage chatMessage;
        if (groupName.contains("$") && (groupName.indexOf('$') == groupName.lastIndexOf('$'))) {
            String groupName2 = groupName.split("\\$")[1] + "$" + groupName.split("\\$")[0];
            ChatMessage chatMessage1 = findMessage(groupName);
            chatMessage = chatMessage1 == null ? findMessage(groupName2) : chatMessage1;
        } else {
            chatMessage = findMessage(groupName);
        }
        if (chatMessage == null) {
            chatMessage = new ChatMessage(list, groupName);
            collection.insertOne(chatMessage);
        } else {
            chatMessage.getMessages().add(singleMessage);
            collection.replaceOne(eq("_id", chatMessage.getId()), chatMessage);
        }
        return findMessage(chatMessage.getGroupName());
    }

    /**
     * Stores MIME type file on server
     * @param message   Contains payload and path of the file
     * @return          Path on the server where the file is saved
     */
    private String storeImageOnServer(String message) {
        String[] split = message.split("\\s+");
        String payload = split[1];
        String ext = FilenameUtils.getExtension(split[0]);
        String path = "/home/ubuntu/data/file_" + UUID.randomUUID().toString() + "." + ext;
        try (FileOutputStream imgOutFile = new FileOutputStream(path)) {
            byte[] imgByteArray = Base64.decodeBase64(payload);
            imgOutFile.write(imgByteArray);
        } catch (IOException exc) {
            logger.log(Level.WARNING, "File could not be saved.\n" + exc);
        }
        return path;
    }

    /**
     *
     * @param type  the message type
     * @return True if the message type is MIME type
     */
    private boolean isMiMIEType(MessageType type) {
        return type == MessageType.IMAGE || type == MessageType.VIDEO;
    }

    @Override
    public ChatMessage findMessage(String groupName) {
        return collection.find(eq("groupName", groupName)).first();
    }

    @Override
    public List<SingleMessage> searchMessages(String name) {
        List<SingleMessage> messages = new LinkedList<>();
        FindIterable<ChatMessage> iter = collection.find();
        for (ChatMessage msg : iter) {
            if (msg.getGroupName().contains("$") && msg.getGroupName().contains(name)) {
                messages.addAll(msg.getMessages());
            }
        }
        return messages;
    }

    @Override
    public List<SingleMessage> searchMessages(Date time1, Date time2, String groupName) {
        List<SingleMessage> messages = new LinkedList<>();
        ChatMessage chatMessage = findMessage(groupName);
        if(chatMessage == null) {
            return messages;
        }
        else {
            for (SingleMessage msg : chatMessage.getMessages()) {
                if (msg.getTimestamp().compareTo(time1) >= 0 && msg.getTimestamp().compareTo(time2) <= 0) {
                    messages.add(msg);
                }
            }
            return messages;
        }
    }
}
