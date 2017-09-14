package hu.akarnokd.java9.benchmark;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class MulticastPublisherTest {

    final class TestConsumer
            extends CountDownLatch
            implements Flow.Subscriber<Object> {

        final List<Object> list = new ArrayList<>();

        Flow.Subscription s;

        TestConsumer() {
            super(1);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.s = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object item) {
            if (s == null) {
                list.add(Long.MIN_VALUE);
            }
            list.add(item);
        }

        @Override
        public void onError(Throwable throwable) {
            if (s == null) {
                list.add(Long.MIN_VALUE);
            }
            list.add(throwable);
            countDown();
        }

        @Override
        public void onComplete() {
            if (s == null) {
                list.add(Long.MIN_VALUE);
            }
            list.add(Long.MAX_VALUE);
            countDown();
        }
    }

    @Test
    public void normal() {
        MulticastPublisher<Integer> mp = new MulticastPublisher<>(Runnable::run, 128);

        TestConsumer tc = new TestConsumer();

        mp.subscribe(tc);

        assertNotNull(tc.s);

        for (int i = 0; i < 1_000_000; i++) {
            assertTrue(mp.offer(i));
        }
        mp.complete();

        assertEquals(1_000_001, tc.list.size());
        for (int i = 0; i < 1_000_000; i++) {
            assertEquals((Integer)i, tc.list.get(i));
        }
        assertEquals(Long.MAX_VALUE, tc.list.get(1_000_000));
    }


    @Test
    public void async() throws Exception {
        MulticastPublisher<Integer> mp = new MulticastPublisher<>(ForkJoinPool.commonPool(), 128);

        TestConsumer tc = new TestConsumer();

        mp.subscribe(tc);

        for (int i = 0; i < 1_000_000; i++) {
            while(!mp.offer(i)) {
                Thread.yield();
            }
        }
        mp.complete();

        assertTrue(tc.await(5, TimeUnit.SECONDS));

        assertNotNull(tc.s);

        assertEquals(1_000_001, tc.list.size());
        for (int i = 0; i < 1_000_000; i++) {
            assertEquals((Integer)i, tc.list.get(i));
        }
        assertEquals(Long.MAX_VALUE, tc.list.get(1_000_000));
    }
}
