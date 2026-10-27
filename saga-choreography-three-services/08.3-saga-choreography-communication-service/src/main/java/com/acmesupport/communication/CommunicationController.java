package com.acmesupport.communication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CommunicationController {

    private final DeliveryLog deliveryLog;

    public CommunicationController(DeliveryLog deliveryLog) {
        this.deliveryLog = deliveryLog;
    }

    /** Inspection endpoint - the AI-drafted message a customer would have received. */
    @GetMapping("/internal/communications")
    public List<DeliveredMessage> listDelivered() {
        return deliveryLog.all();
    }
}
