package hu.akarnokd.java9.flow.subscribers;

import hu.akarnokd.java9.flow.functionals.AutoDisposable;
import hu.akarnokd.java9.flow.utils.ExceptionHelper;
import hu.akarnokd.java9.flow.utils.SubscriptionHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FlowBlockingSubscriber<T> extends CountDownLatch implements Flow.Subscriber<T>, AutoDisposable {

    protected volatile Flow.Subscription subscription;
    protected static final VarHandle SUBSCRIPTION;

    protected T value;
    protected Throwable error;

    static {
        try {
            SUBSCRIPTION = MethodHandles.lookup().findVarHandle(FlowBlockingSubscriber.class, "subscription", Flow.Subscription.class);
        } catch (Throwable ex) {
            throw new InternalError(ex);
        }
    }

    public FlowBlockingSubscriber() {
        super(1);
    }


    @Override
    public final void onSubscribe(Flow.Subscription subscription) {
        if (SubscriptionHelper.replace(SUBSCRIPTION, this, subscription)) {
            subscription.request(Long.MAX_VALUE);
        }
    }

    public final T blockingGet() {
        if (getCount() != 0) {
            try {
                await();
            } catch (InterruptedException ex) {
                SubscriptionHelper.cancel(SUBSCRIPTION, this);
                throw new RuntimeException(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return value;
    }

    public final T blockingGet(long timeout, TimeUnit unit) {
        if (getCount() != 0) {
            try {
                if (!await(timeout, unit)) {
                    SubscriptionHelper.cancel(SUBSCRIPTION, this);
                    throw new RuntimeException(new TimeoutException());
                }
            } catch (InterruptedException ex) {
                SubscriptionHelper.cancel(SUBSCRIPTION, this);
                throw new RuntimeException(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return value;
    }

    @Override
    public void close() {
        SubscriptionHelper.cancel(SUBSCRIPTION, this);
    }
}
