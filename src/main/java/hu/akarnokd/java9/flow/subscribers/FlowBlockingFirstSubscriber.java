package hu.akarnokd.java9.flow.subscribers;

public final class FlowBlockingFirstSubscriber<T> extends FlowBlockingSubscriber<T> {

    @Override
    public void onNext(T item) {
        if (getCount() != 0L) {
            subscription.cancel();
            value = item;
            countDown();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (getCount() != 0L) {
            value = null;
            error = throwable;
            countDown();
        }
    }

    @Override
    public void onComplete() {
        countDown();
    }
}
