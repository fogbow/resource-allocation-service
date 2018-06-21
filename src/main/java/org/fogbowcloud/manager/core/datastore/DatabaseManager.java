package org.fogbowcloud.manager.core.datastore;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DatabaseManager implements StableStorage {

    private static final String HOST = "localhost";
    private static final int PORT = 27017;
    private static final String DB_NAME = "fogbow";

    private static DatabaseManager instance;

    private MongoDatabase mongoDatabase;

    private DatabaseManager() {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        // propertiesHolder.getProperty();
        this.mongoDatabase = initiateDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    @Override
    public void add(Order order) {
        MongoCollection<Order> orderCollection = this.mongoDatabase.getCollection("order", Order.class);
        orderCollection.insertOne(order);
    }

    @Override
    public void update(Order order) {
        MongoCollection<Order> orderCollection = this.mongoDatabase.getCollection("order", Order.class);
        orderCollection.replaceOne(eq("_id", order.getId()), order);
    }

    @Override
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) {
        if (orderState.equals(OrderState.CLOSED)) {
            // returns only orders with instanceId different than null
        }

        return new SynchronizedDoublyLinkedList();
    }

    private MongoDatabase initiateDatabase() {
        MongoClient mongoClient = new MongoClient(HOST, PORT);

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        return mongoClient.getDatabase(DB_NAME).withCodecRegistry(pojoCodecRegistry);
    }
}
