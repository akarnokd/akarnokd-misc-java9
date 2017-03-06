package hu.akarnokd.java9.flow.utils;

import hu.akarnokd.java9.flow.functionals.AutoDisposable;

import java.lang.invoke.VarHandle;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public enum AutoDisposableHelper implements AutoDisposable {
    INSTANCE;


    @Override
    public void close() {
        // deliberately no-op
    }


    public static boolean close(VarHandle field, Object instance) {
        AutoDisposable o = (AutoDisposable)field.getAcquire(instance);
        if (o != INSTANCE) {
            o = (AutoDisposable)field.getAndSet(instance, INSTANCE);
            if (o != INSTANCE) {
                if (o != null) {
                    o.close();
                }
                return true;
            }
        }
        return false;
    }

    public static boolean replace(VarHandle field, Object instance, AutoDisposable next) {
        for (;;) {
            AutoDisposable o = (AutoDisposable)field.getAcquire(instance);
            if (o == INSTANCE) {
                if (next != null) {
                    next.close();
                }
                return false;
            }
            if (field.compareAndSet(instance, o, next)) {
                return true;
            }
        }
    }

    public static boolean set(VarHandle field, Object instance, AutoDisposable next) {
        for (;;) {
            AutoDisposable o = (AutoDisposable)field.getAcquire(instance);
            if (o == INSTANCE) {
                if (next != null) {
                    next.close();
                }
                return false;
            }
            if (field.compareAndSet(instance, o, next)) {
                if (o != null) {
                    o.close();
                }
                return true;
            }
        }
    }

    public static boolean close(AtomicReference<AutoDisposable> field) {
        AutoDisposable o = field.getAcquire();
        if (o != INSTANCE) {
            o = field.getAndSet(INSTANCE);
            if (o != INSTANCE) {
                if (o != null) {
                    o.close();
                }
                return true;
            }
        }
        return false;
    }

    public static boolean replace(AtomicReference<AutoDisposable> field, AutoDisposable next) {
        for (;;) {
            AutoDisposable o = field.getAcquire();
            if (o == INSTANCE) {
                if (next != null) {
                    next.close();
                }
                return false;
            }
            if (field.compareAndSet(o, next)) {
                return true;
            }
        }
    }

    public static boolean set(AtomicReference<AutoDisposable> field, AutoDisposable next) {
        for (;;) {
            AutoDisposable o = field.getAcquire();
            if (o == INSTANCE) {
                if (next != null) {
                    next.close();
                }
                return false;
            }
            if (field.compareAndSet(o, next)) {
                if (o != null) {
                    o.close();
                }
                return true;
            }
        }
    }

}
