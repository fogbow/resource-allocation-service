package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FogbowManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FogbowManagerApplication.class, args);
	}
	
	@Bean
	public CommandLineRunner demo(OrderRepository repository) {
		return (args) -> {
			// save a couple of customers
			repository.save(new ComputeOrder(OrderState.OPEN, new Token(), new Token(), "reqMember", "provMember",
					new OrderInstance("1"), 0, 2, 2048, 20, "test", new UserData()));
			repository.save(new ComputeOrder(OrderState.OPEN, new Token(), new Token(), "reqMember2", "provMember2",
					new OrderInstance("2"), 0, 2, 2048, 20, "test", new UserData()));

			System.out.println("========================");
			for (Order order : repository.findByType("compute")) {
				System.out.println(order.toString());
			}
		};
	}
}

