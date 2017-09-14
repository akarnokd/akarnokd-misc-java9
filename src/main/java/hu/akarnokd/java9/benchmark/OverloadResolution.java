package hu.akarnokd.java9.benchmark;

import java.util.concurrent.Flow;

public class OverloadResolution {

    interface FxSubscriber<T> extends Flow.Subscriber<T> { }

    abstract class Fx<T> {
        void method(Flow.Subscriber<? super T> f) { }
        void method(FxSubscriber<? super T> f) { }
    }

    <T> void m() {
        Fx<? extends T> fx = new Fx<T>() { };

        fx.method(new FxSubscriber<T>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {

            }

            @Override
            public void onNext(T item) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
    }
}
