# Saga Choreography — Complete Three-Service Bundle

This bundle contains Ticket Service, Billing Service, and Communication Service. Read the README.md in any service directory for the full Windows CMD setup, Kafka topic creation, end-to-end event flow, curl testing scenarios, and troubleshooting guide. The same guide is included in each individual project ZIP so every service archive remains self-contained.

## Start order

1. Start Kafka on `localhost:9092`.
2. Create `ticket-events`, `billing-events`, and `refund-outcomes` using `kafka-topics.bat` commands in the service README.
3. Ensure Ollama is running and pull `llama3.2:latest`.
4. Run each service with `mvn spring-boot:run` in its own terminal.
5. Use the Windows CMD curl commands in the README and poll the Saga endpoint until `PROCESSING` changes to `APPROVED` or `DECLINED`.
