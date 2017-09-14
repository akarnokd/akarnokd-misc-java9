package hu.akarnokd.java9.benchmark;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.*;
import java.util.concurrent.ForkJoinPool;

public final class MulticastPublisher<T> implements Publisher<T>, AutoCloseable {

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
            SUBSCRIBERS = MethodHandles.lookup().findVarHandle(MulticastPublisher.class, "subscribers", InnerSubscription[].class);
            DONE = MethodHandles.lookup().findVarHandle(MulticastPublisher.class, "done", Boolean.TYPE);
        } catch (Exception ex) {
            throw new InternalError(ex);
        }
    }

    public MulticastPublisher() {
        this(ForkJoinPool.commonPool(), Flow.defaultBufferSize());
    }

    public MulticastPublisher(Executor executor, int bufferSize) {
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
        Objects.requireNonNull(subscriber, "subscriber is null");
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

    public boolean hasSubscribers() {
        return subscribers.length != 0;
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

    static final class InnerSubscription<T> implements Subscription, Runnable {

        final Subscriber<? super T> actual;
        final MulticastPublisher<T> parent;
        final Object[] queue;
        final int mask;
        static final VarHandle QUEUE;

        volatile boolean badRequest;
        volatile boolean cancelled;
        static final VarHandle CANCELLED;

        volatile boolean done;
        Throwable error;

        boolean subscribed;
        long emitted;

        volatile long requested;
        static final VarHandle REQUESTED;

        volatile int wip;
        static final VarHandle WIP;

        volatile long producerIndex;
        static final VarHandle PRODUCER_INDEX;

        volatile long consumerIndex;
        static final VarHandle CONSUMER_INDEX;

        InnerSubscription(Subscriber<? super T> actual, int bufferSize, MulticastPublisher<T> parent) {
            this.actual = actual;
            this.queue = new Object[bufferSize];
            this.parent = parent;
            this.mask = bufferSize - 1;
        }

        static {
            try {
                WIP = MethodHandles.lookup().findVarHandle(InnerSubscription.class, "wip", Integer.TYPE);
                PRODUCER_INDEX = MethodHandles.lookup().findVarHandle(InnerSubscription.class, "producerIndex", Long.TYPE);
                CONSUMER_INDEX = MethodHandles.lookup().findVarHandle(InnerSubscription.class, "consumerIndex", Long.TYPE);
                CANCELLED = MethodHandles.lookup().findVarHandle(InnerSubscription.class, "cancelled", Boolean.TYPE);
                QUEUE = MethodHandles.arrayElementVarHandle(Object[].class);
                REQUESTED = MethodHandles.lookup().findVarHandle(InnerSubscription.class, "requested", Long.TYPE);
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
            return 1 + mask + consumerIndex == producerIndex;
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
