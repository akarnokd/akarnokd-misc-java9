package hu.akarnokd.java9.flow;

import hu.akarnokd.reactive4javaflow.processors.MulticastProcessor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MpAsyncRequestR4JFTest {
    private final static int N = 1 << 20;

    private final AtomicInteger numbers = new AtomicInteger();
    private final MulticastProcessor<Integer> pub = new
            MulticastProcessor<>();
    private final ExecutorService pubExecutor =
            Executors.newSingleThreadExecutor();
    private final CountDownLatch finished = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        new MpAsyncRequestR4JFTest().run();
    }

    private void run() throws InterruptedException {
        pub.start();
        pub.subscribe(newSubscriber());
        try {
            System.out.println(finished.await(30, TimeUnit.SECONDS));
        } finally {
            pubExecutor.shutdownNow();
        }
        System.out.println("Finished");
    }

    private Flow.Subscriber<Integer> newSubscriber() {
        return new Flow.Subscriber<>() {

            Flow.Subscription sub;
            int received;

            @Override
            public void onSubscribe(Flow.Subscription s) {
                (this.sub = s).request(N);
                publish();
            }

            @Override
            public void onNext(Integer item) {
                if (++received == N) finished.countDown();
                publish();
                System.out.println(item);
            }

            @Override public void onError(Throwable t) { }
            @Override public void onComplete() { }
        };
    }

    private void publish() {
        int number = numbers.incrementAndGet();
        pubExecutor.execute(() -> {
            if (!pub.tryOnNext(number)) {
                System.out.println("Overflow?");
                throw new InternalError();
            }
        });
        //        pub.offer(number, onDropReportError);
    }
}
