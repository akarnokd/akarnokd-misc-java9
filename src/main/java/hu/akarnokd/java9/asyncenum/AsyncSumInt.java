package hu.akarnokd.java9.asyncenum;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncSumInt<T> implements AsyncEnumerable<Integer> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends Number> selector;

    AsyncSumInt(AsyncEnumerable<T> source, Function<? super T, ? extends Number> selector) {
        this.source = source;
        this.selector = selector;
    }

    @Override
    public AsyncEnumerator<Integer> enumerator() {
        return new SumIntEnumerator<>(source.enumerator(), selector);
    }

    static final class SumIntEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<Integer>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends Number> selector;

        boolean hasValue;
        int sum;

        Integer result;
        boolean done;

        CompletableFuture<Boolean> cf;

        SumIntEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends Number> selector) {
            this.source = source;
            this.selector = selector;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (done) {
                result = null;
                return FALSE;
            }
            cf = new CompletableFuture<>();
            collectSource();
            return cf;
        }

        @Override
        public Integer current() {
            return result;
        }

        void collectSource() {
            if (getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                done = true;
                cf.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                sum += selector.apply(source.current()).intValue();
                hasValue = true;
                collectSource();
            } else {
                done = true;
                if (hasValue) {
                    result = sum;
                    cf.complete(true);
                } else {
                    cf.complete(false);
                }
            }
        }
    }
}