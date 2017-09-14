package hu.akarnokd.java9.flow.utils;

import java.lang.invoke.VarHandle;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public enum SubscriptionHelper implements Flow.Subscription {
    INSTANCE;


    @Override
    public void request(long n) {
        // deliberately no-op
    }

    @Override
    public void cancel() {
        // deliberately no-op
    }


    public static boolean cancel(VarHandle field, Object instance) {
        Flow.Subscription o = (Flow.Subscription)field.getAcquire(instance);
        if (o != INSTANCE) {
            o = (Flow.Subscription)field.getAndSet(instance, INSTANCE);
            if (o != INSTANCE) {
                if (o != null) {
                    o.cancel();
                }
                return true;
            }
        }
        return false;
    }

    public static boolean replace(VarHandle field, Object instance, Flow.Subscription next) {
        for (;;) {
            Flow.Subscription o = (Flow.Subscription)field.getAcquire(instance);
            if (o == INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(instance, o, next)) {
                return true;
            }
        }
    }

    public static boolean set(VarHandle field, Object instance, Flow.Subscription next) {
        for (;;) {
            Flow.Subscription o = (Flow.Subscription)field.getAcquire(instance);
            if (o == INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(instance, o, next)) {
                if (o != null) {
                    o.cancel();
                }
                return true;
            }
        }
    }

    public static boolean cancel(AtomicReference<Flow.Subscription> field) {
        Flow.Subscription o = field.getAcquire();
        if (o != INSTANCE) {
            o = field.getAndSet(INSTANCE);
            if (o != INSTANCE) {
                if (o != null) {
                    o.cancel();
                }
                return true;
            }
        }
        return false;
    }

    public static boolean replace(AtomicReference<Flow.Subscription> field, Flow.Subscription next) {
        for (;;) {
            Flow.Subscription o = field.getAcquire();
            if (o == INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(o, next)) {
                return true;
            }
        }
    }

    public static boolean set(AtomicReference<Flow.Subscription> field, Flow.Subscription next) {
        for (;;) {
            Flow.Subscription o = field.getAcquire();
            if (o == INSTANCE) {
                if (next != null) {
                    next.cancel();
                }
                return false;
            }
            if (field.compareAndSet(o, next)) {
                if (o != null) {
                    o.cancel();
                }
                return true;
            }
        }
    }

}
