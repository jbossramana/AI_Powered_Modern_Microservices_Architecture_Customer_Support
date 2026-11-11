# Tool Execution Interceptor: SecurityException and Global Exception Handling

## 1. Purpose

The `SecurityException` prevents an LLM-requested tool from executing when it violates the application's security policy.

The LLM decides which tool to request, but the Spring application validates the request before executing the actual Java method.

## 2. Where to Add the SecurityException

**File:** `src/main/java/com/example/support/security/ToolExecutionPolicy.java`

Inside the `validate()` method, add a temporary blocking rule at the beginning of the method, before JSON argument validation:

```java
public void validate(String toolName, String arguments) {

    System.out.println("POLICY CHECK: " + toolName);

    // TEMPORARY TEST RULE: Block order-status tool
    if ("checkOrderStatus".equals(toolName)) {
        throw new SecurityException(
                "Order status tool blocked by policy");
    }

    // Continue with existing argument validation
}
```

This rule is for demonstration purposes. It deliberately blocks the `checkOrderStatus` tool, regardless of its arguments.

## 3. How the Exception Works

When the LLM requests `checkOrderStatus`, the following execution sequence occurs:

1. The LLM requests the tool.
2. Spring AI passes the request to `GuardedToolCallback`.
3. `GuardedToolCallback` calls `ToolExecutionPolicy.validate()`.
4. The policy detects that the tool is blocked.
5. `SecurityException` is thrown.
6. The delegate's actual Java method is not executed.

The exception propagates through Spring AI's tool-calling manager unless it is handled within the tool execution flow.

## 4. Create a Global Exception Handler

**Create this file:**

`src/main/java/com/example/support/controller/GlobalExceptionHandler.java`

```java
package com.example.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(
            SecurityException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Tool call rejected by security policy: "
                        + ex.getMessage());
    }
}
```

### Purpose of this class

- Centralizes handling of `SecurityException` exceptions that reach Spring MVC.
- Returns HTTP `403 Forbidden` instead of the default server-error response.
- Provides a concise error message to the REST client.
- Avoids exposing the full Java stack trace in the HTTP response.

The handler does not prevent the exception from propagating through Spring AI. It handles the exception when it reaches the REST layer.

## 5. Test the Interceptor

Restart the Spring Boot application:

```powershell
mvn clean spring-boot:run
```

Execute this command from Windows Command Prompt:

```cmd
curl.exe -i -X POST "http://localhost:8080/api/support/ORD-101" -H "Content-Type: application/json" -d "{\"question\":\"Payment was deducted but the order was not created. Investigate the issue.\"}"
```

## 6. Expected Console Output

```text
POLICY CHECK: checkOrderStatus
```

You should not see these messages for the blocked call:

```text
POLICY APPROVED
Executing order tool: ORD-101
```

This demonstrates that the policy rejected the request before the original tool method ran.

## 7. Expected HTTP Response

If the `SecurityException` propagates to Spring MVC and is handled by `GlobalExceptionHandler`, the response should resemble:

```text
HTTP/1.1 403 Forbidden

Tool call rejected by security policy: Order status tool blocked by policy
```

## 8. Important Considerations

- The blocking rule is temporary and should be removed after testing.
- `SecurityException` is a policy-rejection mechanism, not a replacement for authentication and authorization.
- A global exception handler formats the REST error; it does not make the LLM retry automatically.
- If the LLM must continue investigating after a rejected tool call, the rejection must be converted into a controlled tool result within the Spring AI tool-calling flow.
- In production, validate the authenticated user's access to the requested order before returning sensitive information.

**Key takeaway:** The interceptor creates an application-controlled security boundary between the LLM's decision to request a tool and the execution of the actual Java method.
