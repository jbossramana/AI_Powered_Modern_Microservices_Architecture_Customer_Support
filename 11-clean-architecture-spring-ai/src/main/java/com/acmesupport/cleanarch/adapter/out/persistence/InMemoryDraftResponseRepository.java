package com.acmesupport.cleanarch.adapter.out.persistence;

import com.acmesupport.cleanarch.application.port.out.DraftResponseRepositoryPort;
import com.acmesupport.cleanarch.domain.model.DraftResponse;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDraftResponseRepository implements DraftResponseRepositoryPort {

    private final Map<String, DraftResponse> drafts = new ConcurrentHashMap<>();

    @Override
    public DraftResponse save(DraftResponse draft) {
        drafts.put(draft.id(), draft);
        return draft;
    }

    @Override
    public Optional<DraftResponse> findById(String id) {
        return Optional.ofNullable(drafts.get(id));
    }

    @Override
    public List<DraftResponse> findByStatus(DraftResponse.ApprovalStatus status) {
        return drafts.values().stream()
                .filter(d -> d.status() == status)
                .sorted(Comparator.comparing(DraftResponse::createdAt))
                .toList();
    }
}
