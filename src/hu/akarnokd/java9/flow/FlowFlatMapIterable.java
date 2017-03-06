package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowFunction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public final class FlowFlatMapIterable<T, R> implements FlowAPI<R> {

    final Flow.Publisher<T> source;

    final FlowFunction<? super T, ? extends Iterable<? extends R>> mapper;

    final Executor executor;

    public FlowFlatMapIterable(Flow.Publisher<T> source, FlowFunction<? super T, ? extends Iterable<? extends R>> mapper, Executor executor) {
        this.source = source;
        this.mapper = mapper;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        source.subscribe(new FlatMapIterableSubscriber<>(subscriber, executor, mapper));
    }

    static final class FlatMapIterableSubscriber<T, R>
            extends AtomicInteger
            implements Flow.Subscriber<T>, Flow.Subscription, Runnable {

        final Flow.Subscriber<? super R> actual;

        final Executor executor;

        final FlowFunction<? super T, ? extends Iterable<? extends R>> mapper;

        volatile T current;
        static final VarHandle CURRENT;

        volatile long requested;
        static final VarHandle REQUESTED;

        Iterator<? extends R> currentIterator;

        Flow.Subscription subscription;

        volatile boolean cancelled;
        volatile boolean badRequest;
        volatile boolean done;
        Throwable error;

        boolean hasSubscribed;

        long emitted;

        static {
            try {
                CURRENT = MethodHandles.lookup().findVarHandle(FlatMapIterableSubscriber.class, "current", Object.class);
                REQUESTED = MethodHandles.lookup().findVarHandle(FlatMapIterableSubscriber.class, "requested", Long.TYPE);
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }

        FlatMapIterableSubscriber(Flow.Subscriber<? super R> actual, Executor executor, FlowFunction<? super T, ? extends Iterable<? extends R>> mapper) {
            this.actual = actual;
            this.executor = executor;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            schedule();
        }

        @Override
        public void onNext(T item) {
            CURRENT.setRelease(this, item);
            schedule();
        }

        void schedule() {
            if (getAndIncrement() == 0) {
                executor.execute(this);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
            done = true;
            schedule();
        }

        @Override
        public void onComplete() {
            done = true;
            schedule();
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                badRequest = true;
            } else {
                for (;;) {
                    long r = requested;
                    long u = r + n;
                    if (u < 0L) {
                        u = Long.MAX_VALUE;
                    }
                    if (REQUESTED.compareAndSet(this, r, u)) {
                        break;
                    }
                }
            }
            schedule();
        }

        @Override
        public void cancel() {
            cancelled = true;
            subscription.cancel();
            schedule();
        }

        void clear() {
            CURRENT.set(this, null);
            currentIterator = null;
        }

        @Override
        public void run() {
            Flow.Subscriber<? super R> a = actual;
            if (!hasSubscribed) {
                hasSubscribed = true;
                a.onSubscribe(this);
                subscription.request(1);
            }
            int missed = 1;
            Iterator<? extends R> it = currentIterator;
            long e = emitted;

            for (;;) {
                if (it == null) {
                    if (cancelled) {
                        clear();
                    } else
                    if (badRequest) {
                        cancelled = true;
                        clear();
                        a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                    } else {
                        boolean d = done;
                        T v = (T) CURRENT.getAcquire(this);
                        boolean empty = v == null;

                        if (d && empty) {
                            Throwable ex = error;
                            if (ex != null) {
                                a.onError(ex);
                            } else {
                                a.onComplete();
                            }
                        } else if (!empty) {
                            CURRENT.set(this, null);
                            boolean b;
                            try {
                                it = mapper.apply(v).iterator();
                                b = it.hasNext();
                                if (!b) {
                                    subscription.request(1);
                                } else {
                                    currentIterator = it;
                                }
                            } catch (Throwable ex) {
                                cancelled = true;
                                it = null;
                                clear();
                                a.onError(ex);
                            }
                        }
                    }
                }

                if (it != null) {
                    long r = requested;
                    while (e != r) {
                        if (cancelled) {
                            clear();
                            break;
                        }
                        if (badRequest) {
                            cancelled = true;
                            it = null;
                            clear();

                            a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                            break;
                        }

                        R w;
                        try {
                            w = it.next();
                        } catch (Throwable ex) {
                            cancelled = true;
                            it = null;
                            clear();
                            a.onError(ex);
                            break;
                        }

                        a.onNext(w);


                        if (cancelled) {
                            clear();
                            break;
                        }
                        if (badRequest) {
                            cancelled = true;
                            it = null;
                            clear();

                            a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                            break;
                        }

                        e++;

                        boolean b;
                        try {
                            b = it.hasNext();
                        } catch (Throwable ex) {
                            cancelled = true;
                            it = null;
                            clear();
                            a.onError(ex);
                            break;
                        }

                        if (!b) {
                            it = null;
                            currentIterator = null;
                            subscription.request(1);
                            break;
                        }
                    }
                    if (e == r) {
                        if (cancelled) {
                            clear();
                        } else
                        if (badRequest) {
                            cancelled = true;
                            it = null;
                            clear();

                            a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                        }
                    }
                }

                int w = get();
                if (w == missed) {
                    emitted = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
    }
}
