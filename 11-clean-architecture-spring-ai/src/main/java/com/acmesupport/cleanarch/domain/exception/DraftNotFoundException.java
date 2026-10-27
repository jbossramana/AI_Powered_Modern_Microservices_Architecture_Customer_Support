package com.acmesupport.cleanarch.domain.exception;

public class DraftNotFoundException extends RuntimeException {
    public DraftNotFoundException(String draftId) {
        super("No draft response found with id " + draftId);
    }
}
