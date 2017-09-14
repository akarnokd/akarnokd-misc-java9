package hu.akarnokd.java9.benchmark;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SpZeroRequest {
    @Test
    public void zeroRequest() {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>(Runnable::run, 128);
        Throwable[] err = { null };

        sp.subscribe(new Flow.Subscriber<Integer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(0L);
            }

            @Override
            public void onNext(Integer item) {

            }

            @Override
            public void onError(Throwable throwable) {
                err[0] = throwable;
            }

            @Override
            public void onComplete() {

            }
        });

        assertNotNull(err[0]);
    }

    @Test
    public void requestDrainRace() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            for (int i = 0; i < 1000; i++) {

                int[] count = { 0 };
                Executor runner = r -> {
                    System.out.println("Scheduling");
                    if (++count[0] > 1) {
                        System.out.println("Rejecting");
                        throw new RejectedExecutionException();
                    }
                    r.run();
                };

                SubmissionPublisher<Integer> sp = new SubmissionPublisher<>(runner, 128);

                Flow.Subscription[] sub = { null };

                List<Object> list = new ArrayList<>();

                sp.subscribe(new Flow.Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        sub[0] = subscription;
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("complete");
                    }
                });

                sp.submit(1);

                assertTrue(list.isEmpty());

                AtomicInteger sync = new AtomicInteger(2);
                CountDownLatch cdl = new CountDownLatch(2);

                Runnable r = () -> {
                    sync.decrementAndGet();
                    while (sync.get() != 0);
                    sub[0].request(1);
                    cdl.countDown();
                };

                exec.submit(r);

                r.run();

                assertTrue(cdl.await(5, TimeUnit.SECONDS));

                System.out.println(list);
            }
        } finally {
            exec.shutdownNow();
        }
    }

    Flow.Subscriber<Integer> createSub(Object[] result, int index, CountDownLatch cdl) {
        return new Flow.Subscriber<Integer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {

            }

            @Override
            public void onNext(Integer item) {

            }

            @Override
            public void onError(Throwable throwable) {
                result[index] = throwable;
                cdl.countDown();
            }

            @Override
            public void onComplete() {
                result[index] = "complete";
                cdl.countDown();
            }
        };
    }

    @Test
    public void closeErrorRace() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            for (int i = 0; i < 1000; i++) {
                System.out.println("Round " + i);
                SubmissionPublisher<Integer> sp = new SubmissionPublisher<>();

                CountDownLatch cdl = new CountDownLatch(2);

                Object[] result = { null, null };

                sp.subscribe(createSub(result, 0, cdl));

                Flow.Subscriber<Integer> sb2 = createSub(result, 1, cdl);

                Throwable ex = new RuntimeException();

                AtomicInteger wip = new AtomicInteger(2);

                Runnable r1 = () -> {
                    wip.decrementAndGet();
                    while (wip.get() != 0) ;

                    sp.closeExceptionally(ex);
                    sp.subscribe(sb2);
                };

                exec.submit(r1);

                wip.decrementAndGet();
                while (wip.get() != 0) ;

                sp.close();

                assertTrue(cdl.await(5, TimeUnit.SECONDS));

                assertEquals(result[0], result[1]);
            }
        } finally {
            exec.shutdownNow();
        }
    }
}
