package com.acmesupport.cleanarch.application.port.out;

import com.acmesupport.cleanarch.domain.model.KnowledgeArticle;

import java.util.List;

public interface ResponseGeneratorPort {
    String generateReply(String ticketText, List<KnowledgeArticle> context);
}
