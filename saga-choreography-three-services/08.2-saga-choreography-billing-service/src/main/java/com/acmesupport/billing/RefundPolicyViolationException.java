package com.acmesupport.billing;

public class RefundPolicyViolationException extends RuntimeException {
    public RefundPolicyViolationException(String message) {
        super(message);
    }
}
