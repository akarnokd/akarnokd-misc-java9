package hu.akarnokd.java9.benchmark;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public final class MulticastPublisher2<T> implements Publisher<T>, AutoCloseable {

    final Executor executor;
    final int bufferSize;

    volatile boolean done;
    static final VarHandle DONE;
    Throwable error;

    static final InnerSubscription[] EMPTY = new InnerSubscription[0];
    static final InnerSubscription[] TERMINATED = new InnerSubscription[0];


    volatile InnerSubscription<T>[] subscribers;
    static final VarHandle SUBSCRIBERS;
    static {
        try {
            SUBSCRIBERS = MethodHandles.lookup().findVarHandle(MulticastPublisher2.class, "subscribers", InnerSubscription[].class);
            DONE = MethodHandles.lookup().findVarHandle(MulticastPublisher2.class, "done", Boolean.TYPE);
        } catch (Exception ex) {
            throw new InternalError(ex);
        }
    }

    public MulticastPublisher2(Executor executor, int bufferSize) {
        if ((bufferSize & (bufferSize - 1)) != 0) {
            throw new IllegalArgumentException("Please provide a power-of-two buffer size");
        }
        this.executor = executor;
        this.bufferSize = bufferSize;
        SUBSCRIBERS.setRelease(this, EMPTY);
    }

    public boolean offer(T item) {
        Objects.requireNonNull(item, "item is null");

        InnerSubscription<T>[] a = subscribers;
        synchronized (this) {
            for (InnerSubscription<T> inner : a) {
                if (inner.isFull()) {
                    return false;
                }
            }
            for (InnerSubscription<T> inner : a) {
                inner.offer(item);
                inner.drain(executor);
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public void complete() {
        if (DONE.compareAndSet(this, false, true)) {
            for (InnerSubscription<T> inner : (InnerSubscription<T>[])SUBSCRIBERS.getAndSet(this, TERMINATED)) {
                inner.done = true;
                inner.drain(executor);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void completeExceptionally(Throwable error) {
        if (DONE.compareAndSet(this, false, true)) {
            this.error = error;
            for (InnerSubscription<T> inner : (InnerSubscription<T>[])SUBSCRIBERS.getAndSet(this, TERMINATED)) {
                inner.error = error;
                inner.done = true;
                inner.drain(executor);
            }
        }
    }

    @Override
    public void close() {
        complete();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        InnerSubscription<T> inner = new InnerSubscription<T>(subscriber, bufferSize, this);
        if (!add(inner)) {
            Throwable ex = error;
            if (ex != null) {
                inner.error = ex;
            }
            inner.done = true;
        }
        inner.drain(executor);
    }

    boolean add(InnerSubscription<T> inner) {

        for (;;) {
            InnerSubscription<T>[] a = subscribers;
            if (a == TERMINATED) {
                return false;
            }

            int n = a.length;
            @SuppressWarnings("unchecked")
            InnerSubscription<T>[] b = new InnerSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(InnerSubscription<T> inner) {
        for (;;) {
            InnerSubscription<T>[] a = subscribers;
            int n = a.length;
            if (n == 0) {
                break;
            }

            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == inner) {
                    j = i;
                    break;
                }
            }
            if (j < 0) {
                break;
            }
            InnerSubscription<T>[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new InnerSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                break;
            }
        }
    }

    abstract static class InnerSubscriptionCold<T> {
        final Subscriber<? super T> actual;
        final MulticastPublisher2<T> parent;
        final Object[] queue;
        final int mask;
        static final VarHandle QUEUE;

        volatile boolean badRequest;
        volatile boolean cancelled;
        static final VarHandle CANCELLED;

        volatile boolean done;
        Throwable error;

        boolean subscribed;
        static {
            try {
                QUEUE = MethodHandles.arrayElementVarHandle(Object[].class);
                CANCELLED = MethodHandles.lookup().findVarHandle(InnerSubscriptionCold.class, "cancelled", Boolean.TYPE);
            } catch (Exception ex) {
                throw new InternalError(ex);
            }
        }

        InnerSubscriptionCold(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            this.actual = actual;
            this.queue = new Object[bufferSize];
            this.parent = parent;
            this.mask = bufferSize - 1;
        }
    }

    abstract static class InnerSubscriptionPad1<T> extends InnerSubscriptionCold<T> {
        volatile long p0, p1, p2, p3, p4, p5, p6, p7;
        volatile long q0, q1, q2, q3, q4, q5, q6, q7;
        InnerSubscriptionPad1(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            super(actual, bufferSize, parent);
        }
    }

    abstract static class InnerSubscriptionWip<T> extends InnerSubscriptionPad1<T> {
        volatile int wip;
        static final VarHandle WIP;
        static {
            try {
                WIP = MethodHandles.lookup().findVarHandle(InnerSubscriptionWip.class, "wip", Integer.TYPE);
            } catch (Exception ex) {
                throw new InternalError(ex);
            }
        }
        InnerSubscriptionWip(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            super(actual, bufferSize, parent);
        }
    }

    abstract static class InnerSubscriptionPad2<T> extends InnerSubscriptionWip<T> {
        volatile long p0, p1, p2, p3, p4, p5, p6, p7;
        volatile long q0, q1, q2, q3, q4, q5, q6, q7;
        InnerSubscriptionPad2(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            super(actual, bufferSize, parent);
        }
    }

    abstract static class InnerSubscriptionProducer<T> extends InnerSubscriptionPad2<T> {
        volatile long producerIndex;
        static final VarHandle PRODUCER_INDEX;
        static {
            try {
                PRODUCER_INDEX = MethodHandles.lookup().findVarHandle(InnerSubscriptionProducer.class, "producerIndex", Long.TYPE);
            } catch (Exception ex) {
                throw new InternalError(ex);
            }
        }
        InnerSubscriptionProducer(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            super(actual, bufferSize, parent);
        }
    }

    abstract static class InnerSubscriptionPad3<T> extends InnerSubscriptionProducer<T> {
        volatile long p0, p1, p2, p3, p4, p5, p6, p7;
        volatile long q0, q1, q2, q3, q4, q5, q6, q7;
        InnerSubscriptionPad3(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            super(actual, bufferSize, parent);
        }
    }

    abstract static class InnerSubscriptionConsumer<T> extends InnerSubscriptionPad3<T> {
        long emitted;

        volatile long requested;
        static final VarHandle REQUESTED;


        volatile long consumerIndex;
        static final VarHandle CONSUMER_INDEX;

        static {
            try {
                CONSUMER_INDEX = MethodHandles.lookup().findVarHandle(InnerSubscriptionConsumer.class, "consumerIndex", Long.TYPE);
                REQUESTED = MethodHandles.lookup().findVarHandle(InnerSubscriptionConsumer.class, "requested", Long.TYPE);
            } catch (Exception ex) {
                throw new InternalError(ex);
            }
        }
        InnerSubscriptionConsumer(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            super(actual, bufferSize, parent);
        }
    }


    static final class InnerSubscription<T> extends InnerSubscriptionConsumer<T> implements Subscription, Runnable {

        volatile long p0, p1, p2, p3, p4, p5, p6, p7;
        volatile long q0, q1, q2, q3, q4, q5, q6, q7;

        InnerSubscription(Subscriber<? super T> actual, int bufferSize, MulticastPublisher2<T> parent) {
            super(actual, bufferSize, parent);
        }

        static {
            try {
            } catch (Exception ex) {
                throw new InternalError(ex);
            }
        }

        void offer(T item) {
            Object[] q = queue;
            int m = mask;
            long pi = producerIndex;
            int offset = (int)(pi) & m;

            QUEUE.setRelease(q, offset, item);
            producerIndex = pi + 1;
        }

        T poll() {
            Object[] q = queue;
            int m = mask;
            long ci = consumerIndex;

            int offset = (int)(ci) & m;
            Object o = QUEUE.getAcquire(q, offset);
            if (o != null) {
                QUEUE.setRelease(q, offset, null);
                CONSUMER_INDEX.setRelease(this, ci + 1);
            }
            return (T)o;
        }

        boolean isFull() {
            //return 1 + mask + consumerIndex == producerIndex;
            Object[] q = queue;
            int m = mask;
            long pi = producerIndex;
            int offset = (int)(pi) & m;
            return QUEUE.getAcquire(q, offset) != null;
        }

        void drain(Executor executor) {
            if ((int)WIP.getAndAdd(this, 1) == 0) {
                executor.execute(this);
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                badRequest = true;
                done = true;
            } else {
                for (;;) {
                    long r = requested;
                    long u = r + n;
                    if (u < 0) {
                        u = Long.MAX_VALUE;
                    }
                    if (REQUESTED.compareAndSet(this, r, u)) {
                        break;
                    }
                }
            }
            drain(parent.executor);
        }

        @Override
        public void cancel() {
            if (CANCELLED.compareAndSet(this, false, true)) {
                parent.remove(this);
            }
        }

        void clear() {
            error = null;
            while (poll() != null) ;
        }

        @Override
        public void run() {
            int missed = 1;
            Subscriber<? super T> a = actual;
            Object[] q = queue;
            int m = mask;

            outer:
            for (;;) {

                if (subscribed) {
                    if (cancelled) {
                        clear();
                    } else {
                        long r = requested;
                        long e = emitted;

                        while (e != r) {
                            if (cancelled) {
                                continue outer;
                            }

                            boolean d = done;

                            if (d) {
                                Throwable ex = error;
                                if (ex != null) {
                                    CANCELLED.setRelease(this, true);
                                    a.onError(ex);
                                    continue outer;
                                }
                                if (badRequest) {
                                    CANCELLED.setRelease(this, true);
                                    parent.remove(this);
                                    a.onError(new IllegalArgumentException("§3.9 violated: request was not positive"));
                                    continue outer;
                                }
                            }

                            T v = poll();
                            boolean empty = v == null;

                            if (d && empty) {
                                CANCELLED.setRelease(this, true);
                                a.onComplete();
                                break;
                            }

                            if (empty) {
                                break;
                            }

                            a.onNext(v);

                            e++;
                        }

                        if (e == r) {
                            if (cancelled) {
                                continue outer;
                            }
                            if (done) {
                                Throwable ex = error;
                                if (ex != null) {
                                    CANCELLED.setRelease(this, true);
                                    a.onError(ex);
                                } else
                                if (badRequest) {
                                    CANCELLED.setRelease(this, true);
                                    a.onError(new IllegalArgumentException("§3.9 violated: request was not positive"));
                                } else
                                if (producerIndex == consumerIndex) {
                                    CANCELLED.setRelease(this, true);
                                    a.onComplete();
                                }
                            }
                        }

                        emitted = e;
                    }
                } else {
                    subscribed = true;
                    a.onSubscribe(this);
                }

                int w = wip;
                if (missed == w) {
                    w = (int)WIP.getAndAdd(this, -missed);
                    if (missed == w) {
                        break;
                    }
                }
                missed = w;
            }
        }
    }
}
