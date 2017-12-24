package hu.akarnokd.java9.asyncenum;

import java.util.concurrent.*;
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

    static AsyncEnumerable<Integer> range(int start, int count) {
        return new AsyncRange(start, count);
    }

    default CompletionStage<Boolean> forEach(Consumer<? super T> consumer) {
        return AsyncForEach.forEach(enumerator(), consumer);
    }

    default <R> AsyncEnumerable<R> flatMap(
            Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
        return new AsyncFlatMap<>(this, mapper);
    }
}
