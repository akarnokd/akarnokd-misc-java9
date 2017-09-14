package hu.akarnokd.java9.benchmark;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;

@Test
public class MulticastPublisherTckTest extends PublisherVerification<Integer> {

    public MulticastPublisherTckTest() {
        super(new TestEnvironment(200));
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        MulticastPublisher<Integer> sp = new MulticastPublisher<>();
        ForkJoinPool.commonPool().submit(() -> {
            while (!sp.hasSubscribers()) {
                Thread.yield();
            }
            for (int i = 0; i < elements; i++) {
                while (!sp.offer(i));
            }
            sp.close();
        });
        return toRs(sp);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 100;
    }

    <T> Publisher<T> toRs(Flow.Publisher<T> fp) {
        return rs -> {
            if (rs == null) {
                fp.subscribe(null);
                return;
            }
            fp.subscribe(new Flow.Subscriber<T>() {

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    rs.onSubscribe(new org.reactivestreams.Subscription() {

                        @Override
                        public void request(long l) {
                            subscription.request(l);
                        }

                        @Override
                        public void cancel() {
                            subscription.cancel();
                        }
                    });
                }

                @Override
                public void onNext(T item) {
                    rs.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    rs.onError(throwable);
                }

                @Override
                public void onComplete() {
                    rs.onComplete();
                }
            });
        };
    }

}
