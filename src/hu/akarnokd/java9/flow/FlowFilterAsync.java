package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.AutoDisposable;
import hu.akarnokd.java9.flow.functionals.FlowFunction;
import hu.akarnokd.java9.flow.utils.AutoDisposableHelper;
import hu.akarnokd.java9.flow.utils.SubscriptionHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class FlowFilterAsync<T> implements FlowAPI<T> {

    final Flow.Publisher<T> source;

    final Executor executor;

    final FlowFunction<? super T, ? extends Flow.Publisher<Boolean>> asyncPredicate;

    final int bufferSize;

    public FlowFilterAsync(Flow.Publisher<T> source, Executor executor, FlowFunction<? super T, ? extends Flow.Publisher<Boolean>> asyncPredicate, int bufferSize) {
        this.source = source;
        this.executor = executor;
        this.asyncPredicate = asyncPredicate;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        source.subscribe(new FilterAsyncSubscriber<T>(subscriber, executor, asyncPredicate, bufferSize));
    }

    static final class FilterAsyncSubscriber<T> extends AtomicInteger
    implements Flow.Subscriber<T>, Flow.Subscription, Runnable {

        final Flow.Subscriber<? super T> actual;

        final Executor executor;

        final FlowFunction<? super T, ? extends Flow.Publisher<Boolean>> asyncPredicate;

        final int limit;

        final T[] queue;
        static final VarHandle QUEUE;

        Flow.Subscription subscription;

        boolean hasSubscribed;

        volatile boolean cancelled;
        volatile boolean badRequest;
        volatile boolean done;
        Throwable error;
        Throwable innerError;

        long producerIndex;

        long consumerIndex;

        long emitted;

        int consumed;

        volatile int response;
        static final VarHandle RESPONSE;
        static final int RESPONSE_NONE = 0;
        static final int RESPONSE_STARTED = 1;
        static final int RESPONSE_PASS = 2;
        static final int RESPONSE_SKIP = 3;


        volatile long requested;
        static final VarHandle REQUESTED;

        volatile AutoDisposable inner;
        static final VarHandle INNER;

        static {
            try {
                QUEUE = MethodHandles.arrayElementVarHandle(Object[].class);
                REQUESTED = MethodHandles.lookup().findVarHandle(FilterAsyncSubscriber.class, "requested", Long.TYPE);
                RESPONSE = MethodHandles.lookup().findVarHandle(FilterAsyncSubscriber.class, "response", Integer.TYPE);
                INNER = MethodHandles.lookup().findVarHandle(FilterAsyncSubscriber.class, "inner", AutoDisposable.class);
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }


        FilterAsyncSubscriber(Flow.Subscriber<? super T> actual, Executor executor, FlowFunction<? super T, ? extends Flow.Publisher<Boolean>> asyncPredicate, int bufferSize) {
            this.actual = actual;
            this.executor = executor;
            this.asyncPredicate = asyncPredicate;
            this.limit = bufferSize - (bufferSize >> 2);
            this.queue = (T[])new Object[bufferSize];
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            schedule();
        }

        @Override
        public void onNext(T item) {
            T[] q = queue;
            int m = q.length - 1;
            long pi = producerIndex;
            int offset = (int)pi & m;
            QUEUE.setRelease(q, offset, item);
            producerIndex = pi + 1;
            schedule();
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
            if (n <= 0L) {
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
            AutoDisposableHelper.close(INNER, this);
            schedule();
        }

        void schedule() {
            if (getAndIncrement() == 0) {
                executor.execute(this);
            }
        }

        void innerResult(boolean result) {
            RESPONSE.setRelease(this, result ? RESPONSE_PASS : RESPONSE_SKIP);
            schedule();
        }

        void innerError(Throwable ex) {
            innerError = ex;
            RESPONSE.setRelease(RESPONSE_SKIP);
            schedule();
        }

        @Override
        public void run() {

            Flow.Subscriber<? super T> a = actual;
            T[] q = queue;

            if (!hasSubscribed) {
                hasSubscribed = true;
                a.onSubscribe(this);
                subscription.request(q.length);
            }

            int m = q.length - 1;
            int missed = 1;
            long ci = consumerIndex;
            long e = emitted;
            int f = consumed;
            int lim = limit;

            for (;;) {

                if (cancelled) {
                    Arrays.fill(q, null);
                } else {
                    long r = requested;

                    while (e != r) {
                        if (cancelled) {
                            Arrays.fill(q, null);
                            break;
                        } else
                        if (badRequest) {
                            cancelled = true;
                            Arrays.fill(q, null);
                            a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                            break;
                        }

                        boolean d = done;
                        int offset = (int)ci & m;
                        T v = (T)QUEUE.getAcquire(q, offset);
                        boolean empty = v == null;

                        if (d && empty) {
                            Throwable ex = error;
                            if (ex != null) {
                                a.onError(ex);
                            } else {
                                a.onComplete();
                            }
                            cancelled = true;
                            break;
                        }

                        if (empty) {
                            break;
                        }

                        int state = (int)RESPONSE.getAcquire(this);

                        if (state == RESPONSE_NONE) {
                            RESPONSE.set(this, RESPONSE_STARTED);

                            Flow.Publisher<Boolean> p;

                            try {
                                p = Objects.requireNonNull(asyncPredicate.apply(v), "The asyncPredicate returned a null Publisher");
                            } catch (Throwable ex) {
                                cancelled = true;
                                subscription.cancel();
                                a.onError(ex);
                                break;
                            }

                            InnerSubscriber inner = new InnerSubscriber();
                            if (AutoDisposableHelper.replace(INNER, this, inner)) {
                                p.subscribe(inner);
                            }
                            break;
                        } else if (state == RESPONSE_PASS) {
                            INNER.set(this, null);
                            QUEUE.setRelease(q, offset, null);

                            a.onNext(v);

                            e++;
                            ci++;
                            if (++f == lim) {
                                f = 0;
                                subscription.request(lim);
                            }
                            RESPONSE.set(this, RESPONSE_NONE);
                        } else if (state == RESPONSE_SKIP) {
                            INNER.set(this, null);
                            QUEUE.setRelease(q, offset, null);

                            Throwable ex = innerError;
                            if (ex != null) {
                                cancelled = true;
                                subscription.cancel();
                                a.onError(ex);
                                break;
                            }

                            ci++;
                            if (++f == lim) {
                                f = 0;
                                subscription.request(lim);
                            }
                            RESPONSE.set(this, RESPONSE_NONE);
                        } else {
                            break;
                        }
                    }

                    if (e == r) {
                        if (cancelled) {
                            Arrays.fill(q, null);
                        } else
                        if (badRequest) {
                            cancelled = true;
                            Arrays.fill(q, null);
                            a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                        } else {
                            if (done && QUEUE.getAcquire(q, (int)ci & m) == null) {
                                Throwable ex = error;
                                if (ex != null) {
                                    a.onError(ex);
                                } else {
                                    a.onComplete();
                                }
                                cancelled = true;
                            }
                        }
                    }
                }

                int w = get();
                if (w == missed) {
                    emitted = e;
                    consumed = f;
                    consumerIndex = ci;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        final class InnerSubscriber extends AtomicReference<Flow.Subscription> implements Flow.Subscriber<Boolean>, AutoDisposable {

            boolean hasValue;
            boolean result;
            boolean once;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                if (SubscriptionHelper.replace(this, subscription)) {
                    subscription.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Boolean item) {
                if (hasValue) {
                    cancel();
                } else {
                    hasValue = true;
                    result = item;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!once) {
                    once = true;
                    innerError(throwable);
                }
            }

            @Override
            public void onComplete() {
                if (!once) {
                    once = true;
                    innerResult(hasValue && result);
                }
            }

            @Override
            public void close() {
                SubscriptionHelper.cancel(this);
            }
        }
    }
}
