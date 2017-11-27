package hu.akarnokd.java9.flow;

import hu.akarnokd.reactive4javaflow.*;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.*;

public class SubmissionPublisherSubmitTest {

    @Test
    public void cancelUnblocks() throws Exception {

        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>(
                ForkJoinPool.commonPool(), 1);

        TestConsumer<Integer> tc1 = new TestConsumer<>(0L);
        TestConsumer<Integer> tc2 = new TestConsumer<>(0L);

        sp.subscribe(tc1);
        sp.subscribe(tc2);

        CountDownLatch cdl = new CountDownLatch(1);

        SchedulerServices.single().schedule(() -> {
            for (int i = 0; i < 512; i++) {
                sp.submit(i);
            }
            cdl.countDown();
        });

        Thread.sleep(1000);

        assertEquals(1, cdl.getCount());

        tc1.cancel();
        tc2.cancel();

        assertTrue(cdl.await(5, TimeUnit.SECONDS));
    }
}
