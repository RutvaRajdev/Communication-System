package edu.northeastern.ccs.im.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import edu.northeastern.ccs.im.model.Subpoena;
import edu.northeastern.ccs.im.service.SubpeonaServices;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

class SubpeonaServicesImplTest {

    private static SubpeonaServices subpeonaServices;
    private static MongoServer server;
    private static MongoClient client;
    private static MongoCollection<Subpoena> collection;

    @BeforeEach
    void setUp() throws Exception {
        subpeonaServices = new SubpeonaServicesImpl();
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        client = new MongoClient(new ServerAddress(serverAddress));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        collection = client
                .getDatabase("local")
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection("subpoena", Subpoena.class);
        Field col = SubpeonaServicesImpl.class.getDeclaredField("collection");
        col.setAccessible(true);
        col.set(subpeonaServices, collection);
        subpeonaServices.createSubpoena("u", "pwd","u1", "u2", null);
    }

    @Test
    void createSubpoena() {
        assertEquals("pwd", subpeonaServices.findByName("u").getPassword());
        assertNotNull(subpeonaServices.findByName("u").getId());
        assertNull(subpeonaServices.createSubpoena("u", "pwd","u1", "u2", null));
    }

    @Test
    void findByName() {
        assertEquals("u1", subpeonaServices.findByName("u").getUser1());
        assertEquals("u2", subpeonaServices.findByName("u").getUser2());
    }

    @Test
    void findByUsersMonitored() {
        subpeonaServices.createSubpoena("u1", "pwd","u11", "u22", null);
        subpeonaServices.createSubpoena("u2", "pwd","u11", "u22", null);
        subpeonaServices.createSubpoena("u3", "pwd","u11", "u22", null);
        assertEquals(3, subpeonaServices.findByUsersMonitored("u11", "u22").size());
        subpeonaServices.createSubpoena("u1", "pwd","u11", "u22", null);
        subpeonaServices.createSubpoena("u2", "pwd","u11", "u22", null);
        subpeonaServices.createSubpoena("u3", "pwd","u11", "u22", null);
        assertEquals(3, subpeonaServices.findByUsersMonitored("u11", "u22").size());
        subpeonaServices.createSubpoena("u1", "pwd","u11", "u22", null);
        subpeonaServices.createSubpoena("u2", "pwd","u11", "u22", null);
        subpeonaServices.createSubpoena("u3", "pwd","u11", "u22", null);
        assertEquals(3, subpeonaServices.findByUsersMonitored("u11", "u22").size());
    }

    @Test
    void findByGroupMonitored() {
        subpeonaServices.createSubpoena("u11", "pwd",null, null, "grp");
        subpeonaServices.createSubpoena("u12", "pwd",null, null, "grp");
        subpeonaServices.createSubpoena("u13", "pwd",null, null, "grp");
        assertEquals(3, subpeonaServices.findByGroupMonitored("grp").size());
        subpeonaServices.createSubpoena("u11", "pwd",null, null, "grp");
        subpeonaServices.createSubpoena("u12", "pwd",null, null, "grp");
        subpeonaServices.createSubpoena("u13", "pwd",null, null, "grp");
        subpeonaServices.createSubpoena("u11", "pwd",null, null, "grp");
        subpeonaServices.createSubpoena("u12", "pwd",null, null, "grp");
        subpeonaServices.createSubpoena("u13", "pwd",null, null, "grp");
        assertEquals(3, subpeonaServices.findByGroupMonitored("grp").size());
    }
}