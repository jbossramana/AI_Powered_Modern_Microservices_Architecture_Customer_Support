package com.example.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupTool {
	private static final Logger log = LoggerFactory.getLogger(AccountLookupTool.class);

	@Tool(description = "Look up a customer's account status using their customer ID. Use when the customer asks about their account.")
	public String lookupAccount(@ToolParam(description = "Customer ID, for example CUST-1001") String customerId) {
		log.info("[TOOL EXECUTION] lookupAccount arguments={{\"customerId\":\"{}\"}}", customerId);
		String result = switch (customerId.toUpperCase()) {
		case "CUST-1001" ->
			"{\"customerId\":\"CUST-1001\",\"accountStatus\":\"ACTIVE\",\"plan\":\"Premium\",\"region\":\"APAC\"}";
		case "CUST-1002" ->
			"{\"customerId\":\"CUST-1002\",\"accountStatus\":\"SUSPENDED\",\"plan\":\"Basic\",\"region\":\"APAC\"}";
		default -> "{\"found\":false,\"customerId\":\"" + customerId + "\"}";
		};
		log.info("[TOOL RESULT] lookupAccount result={}", result);
		return result;
	}
}
