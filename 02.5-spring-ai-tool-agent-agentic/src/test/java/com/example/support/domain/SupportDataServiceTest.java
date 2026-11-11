package com.example.support.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupportDataServiceTest {
    private final SupportDataService data = new SupportDataService();

    @Test
    void returnsKnownPlan() {
        assertEquals("Pro", data.getCustomerPlan("C-1001"));
    }

    @Test
    void returnsKnownTicketStatus() {
        assertEquals("OPEN", data.getTicketStatus("TCK-1001"));
    }
}
