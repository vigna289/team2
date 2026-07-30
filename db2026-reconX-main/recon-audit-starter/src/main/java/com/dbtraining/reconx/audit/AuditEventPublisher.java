package com.dbtraining.reconx.audit;

public class AuditEventPublisher {

    public void publish(String event) {
        System.out.println("AUDIT EVENT: " + event);
    }
}