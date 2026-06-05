package com.quant.common.security;

public final class ServiceActorContext {

    private static final ThreadLocal<ServiceActor> HOLDER = new ThreadLocal<>();

    public static void set(ServiceActor actor) {
        HOLDER.set(actor);
    }

    public static ServiceActor get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private ServiceActorContext() {
    }
}
