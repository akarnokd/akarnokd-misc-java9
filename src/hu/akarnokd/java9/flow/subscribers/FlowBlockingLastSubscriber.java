package hu.akarnokd.java9.flow.subscribers;

public final class FlowBlockingLastSubscriber<T> extends FlowBlockingSubscriber<T> {

    @Override
    public void onNext(T item) {
        value = item;
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
