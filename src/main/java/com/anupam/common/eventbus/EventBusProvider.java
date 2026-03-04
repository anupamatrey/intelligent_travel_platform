package com.anupam.common.eventbus;

import com.google.common.eventbus.EventBus;

/**
 * Singleton provider for a shared EventBus instance used across agents
 */
public final class EventBusProvider {
    private static final EventBus EVENT_BUS = new EventBus("intelligent-travel-platform-bus");

    private EventBusProvider() {}

    public static EventBus getEventBus() {
        return EVENT_BUS;
    }
}

