package org.fogbowcloud.manager.core.models.orders.instances;

public class OrderInstance {

    private String id;

    public OrderInstance(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
