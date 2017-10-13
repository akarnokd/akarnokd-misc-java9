package hu.akarnokd.java9.flow;

import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpPublishAlt {
    @Test
    public void test() throws Exception {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>();

        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            SpConsumer c = new SpConsumer();
            sp.subscribe(c);

            exec.submit(() -> {
                while (c.upstream == null) ;

                int i = 0;
                sp.offer(i, (a, b) -> { throw new RuntimeException(); });

                while (i < SpConsumer.N) {
                    while (c.getAcquire() == i);
                    i++;
                    sp.offer(i, (a, b) -> { throw new RuntimeException(); });
                }
            });

            if (!c.cdl.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out " + c.getAcquire());
            }
        } finally {
            exec.shutdownNow();
        }
    }

    static final class SpConsumer extends AtomicInteger implements Flow.Subscriber<Object> {

        static final int N = 1 << 20;

        final CountDownLatch cdl = new CountDownLatch(1);

        volatile Flow.Subscription upstream;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            upstream = subscription;
            subscription.request(N);
        }

        @Override
        public void onNext(Object item) {
            System.out.println(item);
            int i = getPlain() + 1;
            setRelease(i);
            if (i == N) {
                upstream.cancel();
                cdl.countDown();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            cdl.countDown();
        }

        @Override
        public void onComplete() {
            cdl.countDown();
        }
    }
}
