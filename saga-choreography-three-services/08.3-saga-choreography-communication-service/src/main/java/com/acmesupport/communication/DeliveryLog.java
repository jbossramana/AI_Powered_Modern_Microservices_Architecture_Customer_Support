package com.acmesupport.communication;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DeliveryLog {

    private final List<DeliveredMessage> messages = new CopyOnWriteArrayList<>();

    public void record(DeliveredMessage message) {
        messages.add(message);
    }

    public List<DeliveredMessage> all() {
        return messages;
    }
}
