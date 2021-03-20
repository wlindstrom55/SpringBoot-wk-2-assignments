package com.promineotech.inventoryManagement.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.promineotech.inventoryManagement.entity.Customer;
import com.promineotech.inventoryManagement.entity.Order;
import com.promineotech.inventoryManagement.entity.Product;
import com.promineotech.inventoryManagement.repository.CustomerRepository;
import com.promineotech.inventoryManagement.repository.OrderRepository;
import com.promineotech.inventoryManagement.repository.ProductRepository;
import com.promineotech.inventoryManagement.util.MembershipLevel;
import com.promineotech.inventoryManagement.util.OrderStatus;

@Service
public class OrderService {

		private static final Logger logger = LogManager.getLogger(OrderService.class);
		private final int DELIVERY_DAYS = 7;
		
		@Autowired
		private OrderRepository repo;
		
		@Autowired
		private CustomerRepository customerRepo;
		
		@Autowired
		private ProductRepository productRepo;
		
		public Order submitNewOrder(Set<Long> productIds, Long customerId) throws Exception {
			try {
				Customer customer = customerRepo.findOne(customerId);
				Order order = initializeNewOrder(productIds, customer);
				return repo.save(order);
			} catch (Exception e) {
				logger.error("Exception occurred while tring to create new order for customer: " + customerId, e);
				throw e;
			}
		}
		
		public Order cancelOrder(Long orderId) throws Exception {
			try {
				Order order = repo.findOne(orderId);
				order.setStatus(OrderStatus.CANCELLED);
				return repo.save(order);
			} catch (Exception e) {
				logger.error("Exception occurred while trying to cancel order: " + orderId, e);
				throw new Exception("Unable to update order.");
			}
		}
		
		public Order completeOrder(Long orderId) throws Exception {
			try {
				Order order = repo.findOne(orderId);
				order.setStatus(OrderStatus.DELIVERED);
				order.setDelivered( LocalDate.now() ); //had to add this one in 
				return repo.save(order);
			} catch (Exception e) {
				logger.error("Exception occurred while trying to complete order: " + orderId, e);
				throw new Exception("Unable to update order.");
			}
		}
		
		private Order initializeNewOrder(Set<Long> productIds, Customer customer) {
			Order order = new Order();
			order.setProducts(convertToProductSet(productRepo.findAll(productIds)));
			order.setOrdered(LocalDate.now());
			order.setEstimatedDelivery(LocalDate.now().plusDays(DELIVERY_DAYS));
			order.setCustomer(customer);
			order.setInvoiceAmount(calculateOrderTotal(order.getProducts(), customer.getLevel()));
			order.setStatus(OrderStatus.ORDERED);
			addOrderToProducts(order);
			return order;
		}
		
		//gets the products, for each product, makes sure order is added to product.
		//makes the relationship fully symmetric
		private void addOrderToProducts(Order order) {
			Set<Product> products = order.getProducts();
			for (Product product : products) {
				product.getOrders().add(order);
			}
		}
		
		private Set<Product> convertToProductSet(Iterable<Product> iterable) {
			Set<Product> set = new HashSet<Product>();
			for (Product product : iterable) {
				set.add(product);
			}
			return set;
		}
		
		private double calculateOrderTotal(Set<Product> products, MembershipLevel level) {
			double total = 0;
			for (Product product : products) {
				total += product.getPrice();
			}
			return total - total * level.getDiscount();
		}
}
