package ru.otus.cachehw;

public enum CacheEvent {

    PUT("put"), GET("get"), REMOVE("remove");

    private final String event;

    CacheEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }
}
