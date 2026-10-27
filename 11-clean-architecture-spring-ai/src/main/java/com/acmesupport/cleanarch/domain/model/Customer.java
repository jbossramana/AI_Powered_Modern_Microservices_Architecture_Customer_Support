package com.acmesupport.cleanarch.domain.model;

/**
 * A domain entity. No Spring, no Jakarta persistence annotations, no Jackson
 * annotations - just data and the types that make up its shape. How a Customer gets
 * persisted or serialized is an adapter's problem, not this class's.
 */
public record Customer(String id, String name, String email, PlanTier plan) {

    public enum PlanTier {
        FREE, PRO, BUSINESS
    }
}
