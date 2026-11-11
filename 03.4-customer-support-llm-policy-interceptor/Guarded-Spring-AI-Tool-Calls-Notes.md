# Customer Support Agent — Guarded Spring AI Tool Calls

## 1. Learning Objectives

After completing this lab, students will understand:

- How an LLM decides which tool to call.
- How Spring AI executes an LLM-requested tool.
- How to intercept a tool call before the actual Java method executes.
- How to validate tool names and arguments.
- How to block a tool call using `SecurityException`.
- The difference between an LLM's decision and application-enforced security.

## 2. What Is Tool Calling?

Tool calling allows an LLM to request the execution of a Java method when it needs external information or functionality.

For example, in a customer support application, the LLM may need to check payment status and order status.

Available tools:

- `checkPaymentStatus(orderId)`
- `checkOrderStatus(orderId)`

The LLM selects a tool based on the user's question and the available tool definitions.

**Important:** The LLM does not directly execute the Java method. It generates a tool-call request, and Spring AI handles the execution.

## 3. What Is a Guarded Tool Call?

A guarded tool call adds an application-controlled validation step before executing the actual Java tool.

Instead of allowing every requested tool to execute immediately, the application checks whether the request is permitted.

For example, the application may:

- Reject an unknown tool.
- Reject malformed arguments.
- Validate an order ID.
- Verify whether the authenticated user can access the requested order.
- Block sensitive operations that require additional approval.

This approach is useful when building secure enterprise AI applications.

## 4. Architecture and Execution Flow

```text
        User Question
              |
              v
             LLM
              |
              | Requests a tool
              v
          Spring AI
              |
              v
     GuardedToolCallback
              |
              v
    ToolExecutionPolicy
              |
        +-----+-----+
        |           |
        v           v
     Approved     Rejected
        |           |
        v           v
   Actual Java   SecurityException
      Tool       is propagated
        |
        v
     Tool Result
        |
        v
        LLM
        |
        v
   Final Response
```

The diagram illustrates the successful execution path and the rejection path. In this lab, a rejected exception may terminate the request rather than return control to the LLM.

## 5. Responsibilities of Each Component

### A. LLM

The LLM interprets the user's question and decides whether to request an available tool.

Example:

> "Payment was deducted, but the order was not created. Please investigate."

The model might request `checkPaymentStatus` and `checkOrderStatus`.

The model's choice is not guaranteed to follow a particular sequence unless the application enforces that sequence.

### B. Spring AI Tool-Calling Mechanism

Spring AI receives the model's tool-call request and routes it to the registered tool callback.

Spring AI coordinates the interaction between the model and the Java application.

### C. GuardedToolCallback

File: `security/GuardedToolCallback.java`

This class wraps the original tool callback.

Its responsibility is to invoke the policy before delegating to the actual tool.

Conceptually:

```java
policy.validate(toolName, arguments);

// Execute only if validation succeeds
return delegate.call(arguments);
```

If validation throws an exception, the delegate is not called.

This is an example of the **Decorator design pattern**: the wrapper adds behavior to an existing callback without changing the original tool implementation.

### D. ToolExecutionPolicy

File: `security/ToolExecutionPolicy.java`

This component contains the validation rules.

Examples include:

- Checking whether the tool name is permitted.
- Ensuring arguments are valid JSON.
- Checking that an order ID follows the required format.
- Applying application-specific authorization rules.

A successful validation allows execution to continue. A failed validation rejects the request.

### E. Original Java Tool

Examples:

- `PaymentTools.checkPaymentStatus()`
- `OrderTools.checkOrderStatus()`

These methods perform the actual business operation, such as retrieving a payment or order status.

They should execute only after the configured policy approves the request.

## 6. Understanding SecurityException

A `SecurityException` can be used in this lab to signal that a requested operation violates a policy.

Example:

```java
if ("checkOrderStatus".equals(toolName)) {
    throw new SecurityException(
            "Order status tool blocked by policy");
}
```

When this rule is active:

1. The LLM requests `checkOrderStatus`.
2. The guarded callback invokes the policy.
3. The policy throws `SecurityException`.
4. The original `checkOrderStatus()` method does not execute.
5. The exception propagates unless handled by the application.

This demonstrates that the LLM cannot bypass a policy enforced in the Java execution path.

A `SecurityException` is not a substitute for authentication or authorization. In a production application, the policy should verify the caller's permissions against trusted application data.

## 7. What Is the Role of GlobalExceptionHandler?

File: `controller/GlobalExceptionHandler.java`

A global exception handler processes exceptions that propagate to Spring MVC.

For example, it can convert an unhandled `SecurityException` into an HTTP `403 Forbidden` response.

The handler:

- Provides a consistent HTTP error response.
- Avoids returning a raw stack trace to the client.
- Centralizes REST-layer exception handling.

However, it does not automatically instruct the LLM to retry. If the model should continue after a rejected tool call, the rejection must be handled within the tool-calling flow and returned to the model as a controlled result.

## 8. Prompt Instructions vs. Application Enforcement

A prompt might tell the LLM:

> "Do not call the order-status tool."

This is an instruction to the model. It is not a guaranteed security boundary.

An application-side policy checks the actual tool request before execution.

| Prompt instruction | Guarded tool callback |
|---|---|
| Guides the LLM's behavior | Enforces an application-side rule |
| The model may not follow it | A rejected call cannot reach the delegate through the guarded callback |
| Useful for task guidance | Useful for validation and access control |
| Not sufficient for security | Must be combined with proper authorization and secure registration |

## 9. Practical Lab Exercises

### Exercise 1: Allow valid calls

Allow both payment and order-status tools to execute when their arguments pass validation.

Expected result: the relevant policy checks and tool execution logs appear.

### Exercise 2: Block a tool

Temporarily block `checkOrderStatus` using `SecurityException`.

Expected result: the policy check appears, but the original order tool does not execute.

### Exercise 3: Reject invalid arguments

Send an invalid order ID, such as `INVALID`.

Expected result: if the LLM passes that value to the tool, the policy rejects it before delegation.

### Exercise 4: Reject unknown tools

Attempt to validate a tool name that is not on the application's allowlist.

Expected result: the policy rejects the request.

Note: the model can request only tools exposed through its available tool definitions. An unknown-tool test may require a direct policy unit test rather than a real model request.

### Exercise 5: Add authorization

Extend the policy to check whether the authenticated user is permitted to access the requested order.

Expected result: users can access only the orders they are authorized to view.

## 10. Key Takeaways

1. The LLM chooses which available tool to request.
2. Spring AI coordinates tool-call execution.
3. A guarded callback creates a validation point before the actual Java method.
4. The policy can approve or reject tool requests.
5. `SecurityException` can prevent a rejected tool from executing.
6. A global exception handler formats errors at the REST boundary.
7. Tool-call validation, authentication, and authorization are separate concerns.
8. The LLM controls its decisions, but the application must control access to business operations.

**Final concept:** The purpose of guarded Spring AI tool calls is to combine the flexibility of LLM-driven tool selection with application-enforced validation and security.
