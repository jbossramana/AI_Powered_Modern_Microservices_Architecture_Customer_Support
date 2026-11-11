# Spring AI + Spring Boot + Ollama: Tool Calling with Different Data Types

This example demonstrates a Spring Boot REST endpoint that sends a user
request to an Ollama-hosted LLM. The model can request Java tool calls,
Spring AI invokes the methods, and the tool results are returned to the
model so it can produce a final natural-language answer.

## What this example demonstrates

-   REST request JSON mapped to a Java `CustomerRequest` record.
-   Tool arguments using `Long`, `Double`, and `Integer`.
-   Tool return values using a `Customer` record, `Double`, `Boolean`,
    and `List<String>`.
-   `ChatClient` tool calling with `.tools(customerTools)`.
-   A final LLM response returned as a `String` through `.content()`.

> **Version note:** The code follows the Spring AI 2.0 API style and the
> Spring Boot 4.1 version combination discussed in the example. Before
> building, verify that the exact Spring Boot and Spring AI versions you
> use are available and compatible in the official release
> documentation.

## Prerequisites

1.  Java 17 or newer (use the Java version required by your chosen
    Spring Boot release).
2.  Maven.
3.  Ollama installed and running locally.
4.  A tool-capable model pulled into Ollama.

Pull the example model:

``` bash
ollama pull qwen2.5
```

If needed, start Ollama using your operating system's normal Ollama
application/service. The default local API URL is
`http://localhost:11434`.

## Project structure

``` text
src/main/java/com/example/demo/
  Customer.java
  CustomerRequest.java
  CustomerTools.java
  ChatController.java
  DemoApplication.java

src/main/resources/
  application.properties
pom.xml
```

Use the Java classes from the accompanying example. The main application
class should be in `com.example.demo`:

``` java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## Configuration

`src/main/resources/application.properties`:

``` properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=qwen2.5
```

Ensure `pom.xml` includes the Spring Web starter and Spring AI Ollama
starter, with versions managed by the appropriate Spring AI BOM.

## Run the application

From the directory containing `pom.xml`:

``` bash
mvn spring-boot:run
```

The endpoint is:

``` text
POST http://localhost:8080/api/customer
Content-Type: application/json
```

## Call the endpoint from Windows

### Option A --- Windows PowerShell (recommended)

Use `curl.exe` explicitly because `curl` can be an alias in some
PowerShell versions.

``` powershell
$body = @'
{
  "customerId": 101,
  "price": 1000.0,
  "discountPercentage": 10,
  "checkEligibility": true,
  "requestedProducts": ["Laptop", "Mouse"]
}
'@

curl.exe -X POST "http://localhost:8080/api/customer" `
  -H "Content-Type: application/json" `
  --data-raw $body
```

### Option B --- Windows Command Prompt (`cmd.exe`)

Run this as a single line in Command Prompt:

``` bat
curl.exe -X POST "http://localhost:8080/api/customer" -H "Content-Type: application/json" --data-raw "{\"customerId\":101,\"price\":1000.0,\"discountPercentage\":10,\"checkEligibility\":true,\"requestedProducts\":[\"Laptop\",\"Mouse\"]}"
```

### Expected request JSON

``` json
{
  "customerId": 101,
  "price": 1000.0,
  "discountPercentage": 10,
  "checkEligibility": true,
  "requestedProducts": ["Laptop", "Mouse"]
}
```

### Example response

The exact wording depends on the model and the tool calls it chooses. A
possible response is:

``` text
Customer Ram (ID 101) is active.

Original price: 1000.00
Discount: 10%
Discounted price: 900.00
Order eligible: Yes

Requested products: Laptop and Mouse.
```

## How tool calling works

1.  Spring Boot/Jackson maps the HTTP JSON to `CustomerRequest`.
2.  The controller builds a prompt and passes `CustomerTools` using
    `.tools(customerTools)`.
3.  Ollama's model chooses whether to request one or more available
    tools and supplies structured arguments.
4.  Spring AI invokes the Java methods and converts their return values
    into results the model can consume.
5.  The model uses the tool results to generate its final response.
6.  `.content()` returns that final response as a Java `String`.

The model does not directly execute Java methods. Spring AI handles tool
invocation in the application.

## Tool method type summary

  -------------------------------------------------------------------------------------------------------
  Method                                                  Parameters              Return type
  ------------------------------------------------------- ----------------------- -----------------------
  `getCustomer(Long customerId)`                          `Long`                  `Customer`

  `calculateDiscount(Double price, Integer percentage)`   `Double`, `Integer`     `Double`

  `isEligible(Double amount)`                             `Double`                `Boolean`

  `getProducts()`                                         None                    `List<String>`
  -------------------------------------------------------------------------------------------------------

## Troubleshooting

-   **Connection refused:** Check that Ollama is running and that
    `http://localhost:11434` is reachable.
-   **Model not found:** Run `ollama pull qwen2.5` and ensure the
    configured model name matches.
-   **No tool call:** Use a model/version that supports tool calling and
    ask for a task that clearly requires the tools.
-   **JSON or conversion errors:** Ensure the request field names and
    types match `CustomerRequest`; check the application logs.
-   **Build/version errors:** Verify Spring Boot and Spring AI versions
    and their compatibility in the official documentation.
