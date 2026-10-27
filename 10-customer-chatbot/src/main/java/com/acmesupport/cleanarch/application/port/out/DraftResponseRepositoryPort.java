package com.acmesupport.cleanarch.application.port.out;

import com.acmesupport.cleanarch.domain.model.DraftResponse;

import java.util.List;
import java.util.Optional;

public interface DraftResponseRepositoryPort {
    DraftResponse save(DraftResponse draft);

    Optional<DraftResponse> findById(String id);

    List<DraftResponse> findByStatus(DraftResponse.ApprovalStatus status);
}
