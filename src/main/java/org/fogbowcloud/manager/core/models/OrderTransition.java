package org.fogbowcloud.manager.core.models;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "order_transition_tb")
public class OrderTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    @JoinColumn(name = "oder_id")
    @NotNull(message = "Order can not be null.")
    private Order order;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Order state can not be null.")
    @Column(name = "inital_state")
    private OrderState initalState;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Order state can not be null.")
    @Column(name = "tagert_state")
    private OrderState tagertState;

    @Column
    @NotNull(message = "Timestamp can not be null.")
    private Long timestamp;

    public OrderTransition() {
    }

    public OrderTransition(Order order, OrderState initalState, OrderState tagertState, Long timestamp) {
        this.order = order;
        this.initalState = initalState;
        this.tagertState = tagertState;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderState getInitalState() {
        return initalState;
    }

    public void setInitalState(OrderState initalState) {
        this.initalState = initalState;
    }

    public OrderState getTagertState() {
        return tagertState;
    }

    public void setTagertState(OrderState tagertState) {
        this.tagertState = tagertState;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
