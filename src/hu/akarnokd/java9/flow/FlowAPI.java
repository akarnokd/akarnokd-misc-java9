package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.*;
import hu.akarnokd.java9.flow.subscribers.TestFlowSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

public interface FlowAPI<T> extends Flow.Publisher<T> {

    // -------------------------------------------------------------
    // FACTORY METHODS
    // -------------------------------------------------------------

    static <T> FlowAPI<T> just(T item) {
        return just(item, ForkJoinPool.commonPool());
    }

    static <T> FlowAPI<T> just(T item, Executor executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static FlowAPI<Integer> range(int start, int count) {
        return range(start, count, ForkJoinPool.commonPool());
    }

    static FlowAPI<Integer> range(int start, int count, Executor executor) {
        if ((long)start + count > (long)Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        return new FlowRange(start, count, executor);
    }

    @SafeVarargs
    static <T> FlowAPI<T> fromArray(T... items) {
        return fromArray(ForkJoinPool.commonPool(), items);
    }

    @SafeVarargs
    static <T> FlowAPI<T> fromArray(Executor executor, T... items) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> fromIterable(Iterable<? extends T> source) {
        return fromIterable(source, ForkJoinPool.commonPool());
    }

    static <T> FlowAPI<T> fromIterable(Iterable<? extends T> source, Executor executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> fromStream(Stream<? extends T> source) {
        return fromStream(source, ForkJoinPool.commonPool());
    }

    static <T> FlowAPI<T> fromStream(Stream<? extends T> source, Executor executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> fromFuture(CompletionStage<? extends T> cs) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static FlowAPI<Long> interval(long period, TimeUnit unit, ScheduledExecutorService executor) {
        return interval(period, period, unit, executor);
    }

    static FlowAPI<Long> interval(long initialDelay, long period, TimeUnit unit, ScheduledExecutorService executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static FlowAPI<Long> timer(long delay, TimeUnit unit, ScheduledExecutorService executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> concat(Iterable<? extends Flow.Publisher<? extends T>> sources) {
        return concat(sources, ForkJoinPool.commonPool());
    }

    static <T> FlowAPI<T> concat(Iterable<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> concatMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources) {
        return concatMany(sources, ForkJoinPool.commonPool());
    }

    static <T> FlowAPI<T> concatMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> merge(Iterable<? extends Flow.Publisher<? extends T>> sources) {
        return merge(sources, ForkJoinPool.commonPool());
    }

    static <T> FlowAPI<T> merge(Iterable<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    static <T> FlowAPI<T> mergeMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources) {
        return mergeMany(sources, ForkJoinPool.commonPool());
    }

    static <T> FlowAPI<T> mergeMany(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources, Executor executor) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // -------------------------------------------------------------------------
    // INSTANCE OPERATORS
    // -------------------------------------------------------------------------

    default <R> FlowAPI<R> map(FlowFunction<? super T, ? extends R> mapper) {
        return map(mapper, ForkJoinPool.commonPool());
    }

    default <R> FlowAPI<R> map(FlowFunction<? super T, ? extends R> mapper, Executor executor) {
        return new FlowMap<>(this, mapper, executor, Flow.defaultBufferSize());
    }

    default FlowAPI<T> filter(FlowPredicate<? super T> predicate) {
        return filter(predicate, ForkJoinPool.commonPool());
    }

    default FlowAPI<T> filter(FlowPredicate<? super T> predicate, Executor executor) {
        return new FlowFilter<>(this, predicate, executor, Flow.defaultBufferSize());
    }

    default FlowAPI<T> take(long n) {
        return take(n, ForkJoinPool.commonPool());
    }

    default FlowAPI<T> take(long n, Executor executor) {
        return new FlowTake<>(this, n, executor, Flow.defaultBufferSize());
    }

    default FlowAPI<T> skip(long n) {
        return skip(n, ForkJoinPool.commonPool());
    }

    default FlowAPI<T> skip(long n, Executor executor) {
        return new FlowTake<>(this, n, executor, Flow.defaultBufferSize());
    }

    default <C> FlowAPI<C> collect(Callable<? extends C> collectionSupplier, FlowConsumer2<? super C, ? super T> collector) {
        return collect(collectionSupplier, collector, ForkJoinPool.commonPool());
    }

    default <C> FlowAPI<C> collect(Callable<? extends C> collectionSupplier, FlowConsumer2<? super C, ? super T> collector, Executor executor) {
        return new FlowCollect<>(this, collectionSupplier, collector, executor, Flow.defaultBufferSize());
    }

    default <R> FlowAPI<R> reduce(Callable<? extends R> initialSupplier, FlowFunction2<R, ? super T, R> reducer) {
        return reduce(initialSupplier, reducer, ForkJoinPool.commonPool());
    }

    default <R> FlowAPI<R> reduce(Callable<? extends R> initialSupplier, FlowFunction2<R, ? super T, R> reducer, Executor executor) {
        return new FlowReduce<>(this, initialSupplier, reducer, executor, Flow.defaultBufferSize());
    }

    default FlowAPI<Integer> sumInt() {
        return sumInt(ForkJoinPool.commonPool());
    }

    default FlowAPI<Integer> sumInt(Executor executor) {
        return new FlowSumInt((Flow.Publisher<Number>)this, executor, Flow.defaultBufferSize());
    }

    default FlowAPI<Long> sumLong() {
        return sumLong(ForkJoinPool.commonPool());
    }

    default FlowAPI<Long> sumLong(Executor executor) {
        return new FlowSumLong((Flow.Publisher<Number>)this, executor, Flow.defaultBufferSize());
    }

    default FlowAPI<Integer> maxInt() {
        return maxInt(ForkJoinPool.commonPool());
    }

    default FlowAPI<Integer> maxInt(Executor executor) {
        return new FlowMaxInt((Flow.Publisher<Number>)this, executor, Flow.defaultBufferSize());
    }

    default FlowAPI<List<T>> toList() {
        return toList(ForkJoinPool.commonPool());
    }

    default FlowAPI<List<T>> toList(Executor executor) {
        return collect(ArrayList::new, (list, t) -> list.add(t), executor);
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

    default AutoCloseable subscribe(FlowConsumer<? super T> onNext) {
        return subscribe(onNext, e -> { }, () -> { });
    }

    default AutoCloseable subscribe(FlowConsumer<? super T> onNext, FlowConsumer<? super Throwable> onError) {
        return subscribe(onNext, e -> { }, () -> { });
    }

    default AutoCloseable subscribe(FlowConsumer<? super T> onNext, FlowConsumer<? super Throwable> onError, FlowAction onComplete) {
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
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default T blockingFirst(T defaultItem) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default T blockingLast() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    default T blockingLast(T defaultItem) {
        // TODO implement
        throw new UnsupportedOperationException();
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
