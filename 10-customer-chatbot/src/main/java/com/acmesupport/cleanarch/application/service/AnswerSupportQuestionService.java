package com.acmesupport.cleanarch.application.service;

import com.acmesupport.cleanarch.application.port.in.AnswerSupportQuestionUseCase;
import com.acmesupport.cleanarch.application.port.out.KnowledgeBaseSearchPort;
import com.acmesupport.cleanarch.application.port.out.ResponseGeneratorPort;
import com.acmesupport.cleanarch.domain.model.KnowledgeArticle;

import java.util.List;

public class AnswerSupportQuestionService implements AnswerSupportQuestionUseCase {

    private final KnowledgeBaseSearchPort knowledgeBase;
    private final ResponseGeneratorPort responseGenerator;

    public AnswerSupportQuestionService(KnowledgeBaseSearchPort knowledgeBase, ResponseGeneratorPort responseGenerator) {
        this.knowledgeBase = knowledgeBase;
        this.responseGenerator = responseGenerator;
    }

    @Override
    public Answer answer(String question) {
        List<KnowledgeArticle> articles = knowledgeBase.search(question, 4);
        String text = responseGenerator.generateReply(question, articles);
        return new Answer(text, articles.stream().map(KnowledgeArticle::title).toList());
    }
}
