package com.acmesupport.cleanarch.application.port.in;

import java.util.List;

public interface AnswerSupportQuestionUseCase {

    Answer answer(String question);

    record Answer(String text, List<String> citedArticleTitles) {
    }
}
