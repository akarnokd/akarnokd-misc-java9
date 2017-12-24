package hu.akarnokd.java9.asyncenum;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

public interface AsyncEnumerable<T> {

    AsyncEnumerator<T> enumerator();

    CompletionStage<Boolean> TRUE = CompletableFuture.completedStage(true);

    CompletionStage<Boolean> FALSE = CompletableFuture.completedStage(false);

    CompletionStage<Boolean> CANCELLED = CompletableFuture.failedStage(new CancellationException() {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    });

    // -------------------------------------------------------------------------------------
    // Static factories
    static AsyncEnumerable<Integer> range(int start, int count) {
        return new AsyncRange(start, count);
    }

    static <T> AsyncEnumerable<T> empty() {
        return AsyncEmpty.instance();
    }

    @SafeVarargs
    static <T> AsyncEnumerable<T> fromArray(T... array) {
        return new AsyncFromArray<>(array);
    }

    static <T> AsyncEnumerable<T> fromIterable(Iterable<T> iterable) {
        return new AsyncFromIterable<>(iterable);
    }

    @SafeVarargs
    static <T> AsyncEnumerable<T> concat(AsyncEnumerable<T>... sources) {
        return new AsyncConcatArray<>(sources);
    }

    static AsyncEnumerable<Integer> characters(CharSequence chars) {
        return new AsyncFromCharSequence(chars);
    }

    // -------------------------------------------------------------------------------------
    // Instance transformations

    default <R> AsyncEnumerable<R> flatMap(
            Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
        return new AsyncFlatMap<>(this, mapper);
    }

    default AsyncEnumerable<T> take(long n) {
        return new AsyncTake<>(this, n);
    }

    default AsyncEnumerable<T> skip(long n) {
        return new AsyncSkip<>(this, n);
    }

    default <R> AsyncEnumerable<R> map(Function<? super T, ? extends R> mapper) {
        return new AsyncMap<>(this, mapper);
    }

    default AsyncEnumerable<T> filter(Predicate<? super T> predicate) {
        return new AsyncFilter<>(this, predicate);
    }

    default <C> AsyncEnumerable<C> collect(Supplier<C> collection, BiConsumer<C, T> collector) {
        return new AsyncCollect<>(this, collection, collector);
    }

    default AsyncEnumerable<Long> sumLong(Function<? super T, ? extends Number> selector) {
        return new AsyncSumLong<>(this, selector);
    }

    default AsyncEnumerable<Integer> sumInt(Function<? super T, ? extends Number> selector) {
        return new AsyncSumInt<>(this, selector);
    }

    default AsyncEnumerable<T> max(Comparator<? super T> comparator) {
        return new AsyncMax<>(this, comparator);
    }

    // -------------------------------------------------------------------------------------
    // Instance consumers

    default CompletionStage<Boolean> forEach(Consumer<? super T> consumer) {
        return AsyncForEach.forEach(enumerator(), consumer);
    }

    default T blockingFirst() {
        AsyncEnumerator<T> en = enumerator();
        try {
            Boolean result = en.moveNext().toCompletableFuture().get();
            if (result) {
                // TODO cancel rest
                return en.current();
            }
            throw new NoSuchElementException();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }
}
