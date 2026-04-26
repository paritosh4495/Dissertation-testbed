package com.dissertation.inventoryservice.fault;


public interface Fault {

    String getId();


    String getDescription();


    boolean isActive();

    void activate();


    void deactivate();
}
