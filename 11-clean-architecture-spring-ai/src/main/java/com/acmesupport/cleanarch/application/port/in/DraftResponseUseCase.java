package com.acmesupport.cleanarch.application.port.in;

import com.acmesupport.cleanarch.domain.model.DraftResponse;

public interface DraftResponseUseCase {

    DraftResponse draft(DraftCommand command);

    record DraftCommand(String ticketId, String customerId, String ticketText) {
    }
}
