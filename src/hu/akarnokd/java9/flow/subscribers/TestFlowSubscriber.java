package hu.akarnokd.java9.flow.subscribers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestFlowSubscriber<T> implements Flow.Subscriber<T> {

    protected final List<T> values;

    protected final List<Throwable> errors;

    protected int completions;

    protected Flow.Subscription subscription;

    protected final CountDownLatch done;

    public TestFlowSubscriber() {
        this.values = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.done = new CountDownLatch(1);
    }

    public TestFlowSubscriber(long initialRequest) {
        this();
        // FIXME incorporate initial requests
    }

    @Override
    public final void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        onStart();
    }

    public void onStart() {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        values.add(item);
    }

    @Override
    public void onError(Throwable throwable) {
        errors.add(throwable);
        done.countDown();
    }

    @Override
    public void onComplete() {
        completions++;
        done.countDown();
    }

    public final void cancel() {
        // FIXME implement deferred cancellation
    }

    public final List<T> values() {
        return values;
    }

    public final List<Throwable> errors() {
        return errors;
    }

    public final int completions() {
        return completions;
    }

    public final boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return done.await(timeout, unit);
    }

    public final TestFlowSubscriber<T> assertResult(T... items) {
        if (!values.equals(Arrays.asList(items))) {
            throw new AssertionError("Expected: " + Arrays.toString(items) + ", Actual: " + values);
        }
        if (completions != 1) {
            throw new AssertionError("Not completed: " + completions);
        }
        return this;
    }

    public final TestFlowSubscriber<T> awaitDone(long timeout, TimeUnit unit) {
        try {
            if (!done.await(timeout, unit)) {
                subscription.cancel();
                throw new RuntimeException("Timed out. Values: " + values.size()
                        + ", Errors: " + errors.size() + ", Completions: " + completions);
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interrupted");
        }
        return this;
    }

    public final TestFlowSubscriber<T> assertRange(int start, int count) {
        if (values.size() != count) {
            throw new AssertionError("Expected: " + count + ", Actual: " + values.size());
        }
        for (int i = 0; i < count; i++) {
            if ((Integer)values.get(i) != start + i) {
                throw new AssertionError("Index: " + i + ", Expected: "
                        + (i + start) + ", Actual: " +values.get(i));
            }
        }
        if (completions != 1) {
            throw new AssertionError("Not completed: " + completions);
        }
        return this;
    }
}
