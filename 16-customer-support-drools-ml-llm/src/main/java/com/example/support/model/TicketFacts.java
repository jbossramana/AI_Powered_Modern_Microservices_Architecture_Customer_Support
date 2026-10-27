package com.example.support.model;
public class TicketFacts {
    private final String category, priority;
    private final double confidence;
    private final boolean enterpriseCustomer;
    private boolean escalate, humanApprovalRequired;
    private String assignedTeam, reason;
    public TicketFacts(String category,String priority,double confidence,boolean enterpriseCustomer) {
        this.category=category; this.priority=priority; this.confidence=confidence; this.enterpriseCustomer=enterpriseCustomer;
    }
    public String getCategory(){return category;} public String getPriority(){return priority;}
    public double getConfidence(){return confidence;} public boolean isEnterpriseCustomer(){return enterpriseCustomer;}
    public boolean isEscalate(){return escalate;} public void setEscalate(boolean v){escalate=v;}
    public boolean isHumanApprovalRequired(){return humanApprovalRequired;} public void setHumanApprovalRequired(boolean v){humanApprovalRequired=v;}
    public String getAssignedTeam(){return assignedTeam;} public void setAssignedTeam(String v){assignedTeam=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
}
