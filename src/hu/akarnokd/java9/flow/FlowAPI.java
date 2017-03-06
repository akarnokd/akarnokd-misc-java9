package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.*;
import hu.akarnokd.java9.flow.subscribers.FlowBlockingFirstSubscriber;
import hu.akarnokd.java9.flow.subscribers.FlowBlockingLastSubscriber;
import hu.akarnokd.java9.flow.subscribers.TestFlowSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Stream;

public interface FlowAPI<T> extends Flow.Publisher<T> {

    // -------------------------------------------------------------
    // FACTORY METHODS
    // -------------------------------------------------------------

    static <T> FlowAPI<T> just(T item) {
        return just(item, FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> just(T item, Executor executor) {
        Objects.requireNonNull(item, "item is null");
        Objects.requireNonNull(executor, "executor is null");
        return FlowAPIPlugins.onAssembly(new FlowJust<>(item, executor));
    }

    static <T> FlowAPI<T> empty() {
        return empty(FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> empty(Executor executor) {
        Objects.requireNonNull(executor, "executor is null");
        return FlowAPIPlugins.onAssembly(new FlowEmpty<>(executor));
    }

    static FlowAPI<Integer> range(int start, int count) {
        return range(start, count, FlowAPIPlugins.defaultExecutor());
    }

    static FlowAPI<Integer> range(int start, int count, Executor executor) {
        Objects.requireNonNull(executor, "executor is null");
        if (count == 0) {
            return empty();
        }
        if (count == 1) {
            return just(start, executor);
        }
        if ((long)start + count > (long)Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        return FlowAPIPlugins.onAssembly(new FlowRange(start, count, executor));
    }

    static FlowAPI<Integer> characters(CharSequence cs) {
        return characters(cs, FlowAPIPlugins.defaultExecutor());
    }

    static FlowAPI<Integer> characters(CharSequence cs, Executor executor) {
        Objects.requireNonNull(executor, "executor is null");
        return FlowAPIPlugins.onAssembly(new FlowCharacters(cs, executor));
    }

    @SafeVarargs
    static <T> FlowAPI<T> fromArray(T... items) {
        return fromArray(FlowAPIPlugins.defaultExecutor(), items);
    }

    @SafeVarargs
    static <T> FlowAPI<T> fromArray(Executor executor, T... items) {
        Objects.requireNonNull(executor, "executor is null");
        return FlowAPIPlugins.onAssembly(new FlowArray<>(items, executor));
    }

    static <T> FlowAPI<T> fromIterable(Iterable<T> source) {
        return fromIterable(source, FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> fromIterable(Iterable<T> source, Executor executor) {
        Objects.requireNonNull(source, "source is null");
        Objects.requireNonNull(executor, "executor is null");
        return FlowAPIPlugins.onAssembly(new FlowIterable<>(source, executor));
    }

    static <T> FlowAPI<T> fromStream(Stream<T> source) {
        return fromStream(source, FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> fromStream(Stream<T> source, Executor executor) {
        Objects.requireNonNull(source, "source is null");
        Objects.requireNonNull(executor, "executor is null");
        return fromIterable(() -> source.iterator(), executor);
    }

    static <T> FlowAPI<T> fromFuture(CompletionStage<? extends T> cs) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static FlowAPI<Long> interval(long period, TimeUnit unit, ScheduledExecutorService executor) {
        return interval(period, period, unit, executor);
    }

    static FlowAPI<Long> interval(long initialDelay, long period, TimeUnit unit, ScheduledExecutorService executor) {
        Objects.requireNonNull(executor, "executor is null");
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static FlowAPI<Long> timer(long delay, TimeUnit unit, ScheduledExecutorService executor) {
        Objects.requireNonNull(executor, "executor is null");
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> concat(Iterable<? extends Flow.Publisher<? extends T>> sources) {
        return concat(sources, FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> concat(Iterable<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(executor, "executor is null");
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> concatMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources) {
        return concatMany(sources, FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> concatMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(executor, "executor is null");
        // TODO implement
        throw new UnsupportedOperationException();
    }


    @SafeVarargs
    static <T> FlowAPI<T> concatArray(Flow.Publisher<? extends T>... sources) {
        return concatArray(FlowAPIPlugins.defaultExecutor(), sources);
    }

    @SafeVarargs
    static <T> FlowAPI<T> concatArray(Executor executor, Flow.Publisher<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(executor, "executor is null");
        return FlowAPIPlugins.onAssembly(new FlowConcatArray<>(sources, executor, Flow.defaultBufferSize()));
    }

    static <T> FlowAPI<T> merge(Iterable<? extends Flow.Publisher<? extends T>> sources) {
        return merge(sources, FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> merge(Iterable<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(executor, "executor is null");
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> mergeMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources) {
        return mergeMany(sources, FlowAPIPlugins.defaultExecutor());
    }

    static <T> FlowAPI<T> mergeMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(executor, "executor is null");
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // -------------------------------------------------------------------------
    // INSTANCE OPERATORS
    // -------------------------------------------------------------------------

    default <R> FlowAPI<R> map(FlowFunction<? super T, ? extends R> mapper) {
        return map(mapper, FlowAPIPlugins.defaultExecutor());
    }

    default <R> FlowAPI<R> map(FlowFunction<? super T, ? extends R> mapper, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowMap<>(this, mapper, executor, Flow.defaultBufferSize()));
    }

    default FlowAPI<T> filter(FlowPredicate<? super T> predicate) {
        return filter(predicate, FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<T> filter(FlowPredicate<? super T> predicate, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowFilter<>(this, predicate, executor, Flow.defaultBufferSize()));
    }

    default FlowAPI<T> take(long n) {
        return take(n, FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<T> take(long n, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowTake<>(this, n, executor, Flow.defaultBufferSize()));
    }

    default FlowAPI<T> skip(long n) {
        return skip(n, FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<T> skip(long n, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowSkip<>(this, n, executor, Flow.defaultBufferSize()));
    }

    default <C> FlowAPI<C> collect(Callable<? extends C> collectionSupplier, FlowConsumer2<? super C, ? super T> collector) {
        return collect(collectionSupplier, collector, FlowAPIPlugins.defaultExecutor());
    }

    default <C> FlowAPI<C> collect(Callable<? extends C> collectionSupplier, FlowConsumer2<? super C, ? super T> collector, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowCollect<>(this, collectionSupplier, collector, executor, Flow.defaultBufferSize()));
    }

    default <R> FlowAPI<R> reduce(Callable<? extends R> initialSupplier, FlowFunction2<R, ? super T, R> reducer) {
        return reduce(initialSupplier, reducer, FlowAPIPlugins.defaultExecutor());
    }

    default <R> FlowAPI<R> reduce(Callable<? extends R> initialSupplier, FlowFunction2<R, ? super T, R> reducer, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowReduce<>(this, initialSupplier, reducer, executor, Flow.defaultBufferSize()));
    }

    default FlowAPI<Integer> sumInt() {
        return sumInt(FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<Integer> sumInt(Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowSumInt((Flow.Publisher<Number>)this, executor, Flow.defaultBufferSize()));
    }

    default FlowAPI<Long> sumLong() {
        return sumLong(FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<Long> sumLong(Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowSumLong((Flow.Publisher<Number>)this, executor, Flow.defaultBufferSize()));
    }

    default FlowAPI<Integer> maxInt() {
        return maxInt(FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<Integer> maxInt(Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowMaxInt((Flow.Publisher<Number>)this, executor, Flow.defaultBufferSize()));
    }

    default FlowAPI<List<T>> toList() {
        return toList(FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<List<T>> toList(Executor executor) {
        return collect(ArrayList::new, (list, t) -> list.add(t), executor);
    }

    default <R> FlowAPI<R> flatMapIterable(FlowFunction<? super T, ? extends Iterable<? extends R>> mapper) {
        return flatMapIterable(mapper, FlowAPIPlugins.defaultExecutor());
    }

    default <R> FlowAPI<R> flatMapIterable(FlowFunction<? super T, ? extends Iterable<? extends R>> mapper, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowFlatMapIterable<>(this, mapper, executor));
    }

    default FlowAPI<T> filterAsync(FlowFunction<? super T, ? extends Flow.Publisher<Boolean>> asyncPredicate) {
        return filterAsync(asyncPredicate, FlowAPIPlugins.defaultExecutor());
    }

    default FlowAPI<T> filterAsync(FlowFunction<? super T, ? extends Flow.Publisher<Boolean>> asyncPredicate, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowFilterAsync<>(this, executor, asyncPredicate, Flow.defaultBufferSize()));
    }


    default <R> FlowAPI<R> mapAsync(FlowFunction<? super T, ? extends Flow.Publisher<? extends R>> asyncMapper) {
        return mapAsync(asyncMapper, (v, u) -> u, FlowAPIPlugins.defaultExecutor());
    }

    default <R> FlowAPI<R> mapAsync(FlowFunction<? super T, ? extends Flow.Publisher<? extends R>> asyncMapper, Executor executor) {
        return mapAsync(asyncMapper, (v, u) -> u, executor);
    }

    default <U, R> FlowAPI<R> mapAsync(FlowFunction<? super T, ? extends Flow.Publisher<? extends U>> asyncMapper, FlowFunction2<? super T, ? super U, ? extends R> resultMapper) {
        return mapAsync(asyncMapper, resultMapper, FlowAPIPlugins.defaultExecutor());
    }

    default <U, R> FlowAPI<R> mapAsync(FlowFunction<? super T, ? extends Flow.Publisher<? extends U>> asyncMapper, FlowFunction2<? super T, ? super U, ? extends R> resultMapper, Executor executor) {
        return FlowAPIPlugins.onAssembly(new FlowMapAsyncBoth<T, U, R>(this, executor, asyncMapper, resultMapper, Flow.defaultBufferSize()));
    }

    // ----------------------------------------------------------------
    // LEAVING THE REACTIVE WORLD
    // ----------------------------------------------------------------

    default Iterable<T> toIterable() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default Stream<T> toStream() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default AutoDisposable subscribe(FlowConsumer<? super T> onNext) {
        return subscribe(onNext, e -> { }, () -> { });
    }

    default AutoDisposable subscribe(FlowConsumer<? super T> onNext, FlowConsumer<? super Throwable> onError) {
        return subscribe(onNext, e -> { }, () -> { });
    }

    default AutoDisposable subscribe(FlowConsumer<? super T> onNext, FlowConsumer<? super Throwable> onError, FlowAction onComplete) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default void blockingSubscribe(FlowConsumer<? super T> onNext) {
        blockingSubscribe(onNext, e -> { }, () -> { });
    }

    default void blockingSubscribe(FlowConsumer<? super T> onNext, FlowConsumer<? super Throwable> onError) {
        blockingSubscribe(onNext, e -> { }, () -> { });
    }

    default void blockingSubscribe(FlowConsumer<? super T> onNext, FlowConsumer<? super Throwable> onError, FlowAction onComplete) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default T blockingFirst() {
        FlowBlockingFirstSubscriber<T> s = new FlowBlockingFirstSubscriber<>();
        subscribe(s);
        T v = s.blockingGet();
        if (v == null) {
            throw new NoSuchElementException();
        }
        return v;
    }

    default T blockingFirst(T defaultItem) {
        FlowBlockingFirstSubscriber<T> s = new FlowBlockingFirstSubscriber<>();
        subscribe(s);
        T v = s.blockingGet();
        if (v == null) {
            v = defaultItem;
        }
        return v;
    }

    default T blockingLast() {
        FlowBlockingLastSubscriber<T> s = new FlowBlockingLastSubscriber<>();
        subscribe(s);
        T v = s.blockingGet();
        if (v == null) {
            throw new NoSuchElementException();
        }
        return v;
    }

    default T blockingLast(T defaultItem) {
        FlowBlockingLastSubscriber<T> s = new FlowBlockingLastSubscriber<>();
        subscribe(s);
        T v = s.blockingGet();
        if (v == null) {
            v = defaultItem;
        }
        return v;
    }

    default T blockingSingle() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default T blockingSingle(T defaultItem) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default CompletionStage<T> first() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default CompletionStage<T> last() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default CompletionStage<T> single() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // ----------------------------------------------------------------
    // TEST SUPPORT
    // ----------------------------------------------------------------

    default TestFlowSubscriber<T> test() {
        TestFlowSubscriber<T> ts = new TestFlowSubscriber<T>();
        subscribe(ts);
        return ts;
    }

    default TestFlowSubscriber<T> test(boolean cancel) {
        TestFlowSubscriber<T> ts = new TestFlowSubscriber<T>();
        if (cancel) {
            ts.cancel();
        }
        subscribe(ts);
        return ts;
    }

    default TestFlowSubscriber<T> test(long initialRequest) {
        TestFlowSubscriber<T> ts = new TestFlowSubscriber<T>(initialRequest);
        subscribe(ts);
        return ts;
    }

    default <E extends Flow.Subscriber<? super T>> E subscribeWith(E subscriber) {
        subscribe(subscriber);
        return subscriber;
    }
}
