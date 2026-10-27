package com.acmesupport.cleanarch.application.port.out;

import com.acmesupport.cleanarch.domain.model.KnowledgeArticle;

import java.util.List;

public interface KnowledgeBaseSearchPort {
    List<KnowledgeArticle> search(String query, int topK);
}
