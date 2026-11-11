package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerTools {

    private static final Logger log =
            LoggerFactory.getLogger(CustomerTools.class);

    @Tool(description = "Find a customer using their customer ID")
    public Customer getCustomer(
            @ToolParam(description = "Customer ID") Long customerId) {

        log.info("TOOL CALLED: getCustomer(customerId={})", customerId);

        // Demo data; replace with a repository/database lookup.
        Customer customer = new Customer(customerId, "Ram", 1000.0, true);

        log.info("TOOL RESULT: getCustomer() returned {}", customer);

        return customer;
    }

    @Tool(description = """
            Calculate the final price after applying a percentage discount.
            Example: price=1000.0 and discountPercent=10 returns 900.0.
            """)
    public Double calculateDiscount(
            @ToolParam(description = "Original price as a number, e.g. 1000.0")
            Double price,

            @ToolParam(description = "Discount percentage as an integer, e.g. 10")
            Integer discountPercent) {

        log.info("TOOL CALLED: calculateDiscount(price={}, discountPercent={})",
                price, discountPercent);

        return price - (price * discountPercent / 100.0);
    }
    
    @Tool(description = "Check whether an order amount is eligible; minimum amount is 500")
    public Boolean isEligible(
            @ToolParam(description = "Order amount") Double amount) {

        log.info("TOOL CALLED: isEligible(amount={})", amount);

        Boolean eligible = amount >= 500.0;

        log.info("TOOL RESULT: isEligible() returned {}", eligible);

        return eligible;
    }

    @Tool(description = "Return the list of available product names")
    public List<String> getProducts() {

        log.info("TOOL CALLED: getProducts()");

        List<String> products = List.of("Laptop", "Keyboard", "Mouse");

        log.info("TOOL RESULT: getProducts() returned {}", products);

        return products;
    }
}
