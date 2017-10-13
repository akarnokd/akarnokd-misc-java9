package hu.akarnokd.java9.flow;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

public class SpAsyncRequestTest {
    private final static int N = 1 << 20;

    private final AtomicInteger numbers = new AtomicInteger();
    private final SubmissionPublisher<Integer> pub = new
            SubmissionPublisher<>();
    private final ExecutorService pubExecutor =
            Executors.newSingleThreadExecutor();
    private final CountDownLatch finished = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        new SpAsyncRequestTest().run();
    }

    private void run() throws InterruptedException {
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
        BiPredicate<Flow.Subscriber<? super Integer>, Integer>
                onDropReportError = (s, i) -> {
            throw new
                InternalError();
        };
        pubExecutor.execute(() -> pub.offer(number, onDropReportError));
        //        pub.offer(number, onDropReportError);
    }
}
