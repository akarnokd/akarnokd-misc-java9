package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowFunction;
import hu.akarnokd.java9.flow.subscribers.FlowAsyncSubscriber;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowMap<T, R> implements FlowAPI<R> {

    final Flow.Publisher<? extends T> source;

    final FlowFunction<? super T, ? extends R> mapper;

    final Executor executor;

    final int bufferSize;

    public FlowMap(Flow.Publisher<? extends T> source, FlowFunction<? super T, ? extends R> mapper, Executor executor, int bufferSize) {
        this.source = source;
        this.mapper = mapper;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        source.subscribe(new MapSubscriber<>(subscriber, mapper, executor, bufferSize));
    }

    static final class MapSubscriber<T, R> extends FlowAsyncSubscriber<T, R> {

        final FlowFunction<? super T, ? extends R> mapper;

        public MapSubscriber(Flow.Subscriber<? super R> actual, FlowFunction<? super T, ? extends R> mapper, Executor executor, int bufferSize) {
            super(actual, executor, bufferSize);
            this.mapper = mapper;
        }

        @Override
        protected OnItemResult onItem(Flow.Subscriber<? super R> a, T item, long index) throws Exception {
            a.onNext(Objects.requireNonNull(mapper.apply(item), "The mapper returned a null item"));
            return OnItemResult.CONTINUE;
        }
    }
}
