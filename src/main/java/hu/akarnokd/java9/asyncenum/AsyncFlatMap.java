package hu.akarnokd.java9.asyncenum;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

final class AsyncFlatMap<T, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<T> upstream;

    final Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper;

    AsyncFlatMap(AsyncEnumerable<T> upstream, Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
        this.upstream = upstream;
        this.mapper = mapper;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        FlatMapEnumerator<T, R> en = new FlatMapEnumerator<>(upstream.enumerator(), mapper);
        en.moveNextUpstream();
        return en;
    }

    static final class FlatMapEnumerator<T, R> implements AsyncEnumerator<R>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> upstream;

        final Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper;

        final Queue<InnerAsyncEnumerator<R>> queue;

        final AtomicReference<CompletableFuture<Boolean>> next;

        final AtomicInteger wip;

        final AtomicInteger active;

        final ConcurrentMap<InnerAsyncEnumerator<R>, Object> inners;

        final AtomicInteger upstreamWip;

        R current;

        FlatMapEnumerator(AsyncEnumerator<T> upstream, Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
            this.upstream = upstream;
            this.mapper = mapper;
            this.queue = new ConcurrentLinkedQueue<>();
            this.next = new AtomicReference<>();
            this.wip = new AtomicInteger();
            this.active = new AtomicInteger(1);
            this.inners = new ConcurrentHashMap<>();
            this.upstreamWip = new AtomicInteger();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> nx = new CompletableFuture<>();
            next.set(nx);
            drain();
            return nx;
        }

        @Override
        public R current() {
            return current;
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                CompletableFuture<Boolean> nx = next.get();
                if (nx != null) {
                    int n = active.get();
                    InnerAsyncEnumerator<R> inner = queue.peek();

                    if (n == 0 && inner == null) {
                        nx.complete(false);
                        return;
                    }

                    if (inner != null) {
                        queue.poll();
                        next.set(null);
                        current = inner.current();
                        nx.complete(true);
                        inner.moveNext();
                    }
                }
            } while (wip.decrementAndGet() != 0);
        }

        void hasNext(InnerAsyncEnumerator<R> inner) {
            queue.offer(inner);
            drain();
        }

        void finish(InnerAsyncEnumerator<R> inner) {
            inners.remove(inner);
            active.decrementAndGet();
            drain();
        }

        void moveNextUpstream() {
            if (upstreamWip.getAndIncrement() != 0) {
                return;
            }

            do {
                upstream.moveNext().whenComplete(this);
            } while (upstreamWip.decrementAndGet() != 0);
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                // TODO manage errors
            }
            if (aBoolean) {
                T t = upstream.current();
                AsyncEnumerator<? extends R> ae = mapper.apply(t).enumerator();
                InnerAsyncEnumerator<R> inner = new InnerAsyncEnumerator<>(ae, this);
                inners.put(inner, inner);
                active.getAndIncrement();
                inner.moveNext();
                moveNextUpstream();
            } else {
                active.decrementAndGet();
                drain();
            }
        }

        static final class InnerAsyncEnumerator<R> extends AtomicInteger implements BiConsumer<Boolean, Throwable> {

            final AsyncEnumerator<? extends R> source;

            final FlatMapEnumerator<?, R> parent;

            InnerAsyncEnumerator(AsyncEnumerator<? extends R> source, FlatMapEnumerator<?, R> parent) {
                this.source = source;
                this.parent = parent;
            }

            R current() {
                return source.current();
            }

            void moveNext() {
                if (getAndIncrement() != 0) {
                    return;
                }

                do {
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }

            @Override
            public void accept(Boolean hasMore, Throwable throwable) {
                if (throwable != null) {
                    // TODO error management
                    return;
                }
                if (hasMore) {
                    parent.hasNext(this);
                } else {
                    parent.finish(this);
                }
            }
        }
    }
}