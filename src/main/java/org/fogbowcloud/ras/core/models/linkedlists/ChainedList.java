package org.fogbowcloud.ras.core.models.linkedlists;

import org.fogbowcloud.ras.core.models.orders.Order;

public interface ChainedList {

    public void addItem(Order order);

    public void resetPointer();

    public Order getNext();

    public boolean removeItem(Order order);
}
