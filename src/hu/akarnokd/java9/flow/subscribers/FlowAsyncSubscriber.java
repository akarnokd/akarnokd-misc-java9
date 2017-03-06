package hu.akarnokd.java9.flow.subscribers;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public abstract class FlowAsyncSubscriber<T, R> extends FlowAsyncSubscriberConsumer<T, R> {

    volatile long r00, r01, r02, r03, r04, r05, r06, r07;
    volatile long r10, r11, r12, r13, r14, r15, r16, r17;

    public FlowAsyncSubscriber(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }

    protected final void schedule() {
        if (getAndIncrement() == 0) {
            executor.execute(this);
        }
    }

    @Override
    public final void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        schedule();
    }

    @Override
    public final void onNext(T item) {
        T[] a = queue;
        int m = a.length - 1;
        long pi = producerIndex;
        int offset = (int)pi & m;
        QUEUE.setRelease(a, offset, item);
        producerIndex = pi + 1;
        schedule();
    }

    @Override
    public final void onError(Throwable throwable) {
        error = throwable;
        done = true;
        schedule();
    }

    @Override
    public final void onComplete() {
        done = true;
        schedule();
    }

    @Override
    public void cancel() {
        cancelled = true;
        subscription.cancel();
        schedule();
    }
}

abstract class FlowAsyncSubscriberCold<T, R>
        implements Flow.Subscriber<T>, Flow.Subscription, Runnable {

    protected final Flow.Subscriber<? super R> actual;

    protected final Executor executor;

    protected final int limit;

    protected final T[] queue;
    protected static final VarHandle QUEUE;

    // the following fields are written at most once

    protected Flow.Subscription subscription;

    protected volatile boolean cancelled;
    protected volatile boolean badRequest;
    protected volatile boolean done;
    protected Throwable error;

    static {
        try {
            QUEUE = MethodHandles.arrayElementVarHandle(Object[].class);
        } catch (Exception ex) {
            throw new InternalError(ex);
        }
    }

    FlowAsyncSubscriberCold(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        this.actual = actual;
        this.executor = executor;
        this.limit = bufferSize - (bufferSize >> 2);
        this.queue = (T[])new Object[bufferSize];
    }

    protected final boolean isCancelled() {
        return cancelled;
    }

}

abstract class FlowAsyncSubscriberPad1<T, R> extends FlowAsyncSubscriberCold<T, R> {
    volatile long p00, p01, p02, p03, p04, p05, p06, p07;
    volatile long p10, p11, p12, p13, p14, p15, p16, p17;

    FlowAsyncSubscriberPad1(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }
}

abstract class FlowAsyncSubscriberWip<T, R> extends FlowAsyncSubscriberPad1<T, R> {

    protected volatile int wip;
    protected static final VarHandle WIP;
    static {
        try {
            WIP = MethodHandles.lookup().findVarHandle(FlowAsyncSubscriberWip.class, "wip", Integer.TYPE);
        } catch (Exception ex) {
            throw new InternalError(ex);
        }
    }

    FlowAsyncSubscriberWip(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }

    protected final int get() {
        return wip;
    }

    protected final int getAndIncrement() {
        return (int)WIP.getAndAdd(this, 1);
    }

    protected final int addAndGet(int diff) {
        return (int)WIP.getAndAdd(diff) + diff;
    }
}

abstract class FlowAsyncSubscriberPad2<T, R> extends FlowAsyncSubscriberWip<T, R> {
    volatile long p00, p01, p02, p03, p04, p05, p06, p07;
    volatile long p10, p11, p12, p13, p14, p15, p16, p17;

    FlowAsyncSubscriberPad2(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }
}


abstract class FlowAsyncSubscriberProducer<T, R> extends FlowAsyncSubscriberPad2<T, R> {

    protected long producerIndex;

    FlowAsyncSubscriberProducer(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }
}

abstract class FlowAsyncSubscriberPad3<T, R> extends FlowAsyncSubscriberProducer<T, R> {
    volatile long q00, q01, q02, q03, q04, q05, q06, q07;
    volatile long q10, q11, q12, q13, q14, q15, q16, q17;

    FlowAsyncSubscriberPad3(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }
}

abstract class FlowAsyncSubscriberConsumer<T, R> extends FlowAsyncSubscriberPad3<T, R> {
    protected long consumerIndex;

    protected long emitted;
    protected int consumed;
    protected boolean hasSubscribed;

    FlowAsyncSubscriberConsumer(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }
}