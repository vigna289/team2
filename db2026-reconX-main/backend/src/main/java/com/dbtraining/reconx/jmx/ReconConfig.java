package com.dbtraining.reconx.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@ManagedResource(objectName = "reconx:type=ReconConfig")
public class ReconConfig {

    private double priceTolerance = 0.01;

    private boolean cachingEnabled = true;


    @ManagedAttribute
    public double getPriceTolerance() {
        return priceTolerance;
    }


    @ManagedAttribute
    public void setPriceTolerance(double priceTolerance) {
        this.priceTolerance = priceTolerance;
    }


    @ManagedAttribute
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }


    @ManagedAttribute
    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }


    @ManagedOperation
    public void clearCache() {
        System.out.println("Cache cleared");
    }
}