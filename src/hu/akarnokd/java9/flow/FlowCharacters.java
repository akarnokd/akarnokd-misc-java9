package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowGeneratorSubscription;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowCharacters implements FlowAPI<Integer> {

    final CharSequence chars;

    final Executor executor;

    public FlowCharacters(CharSequence chars, Executor executor) {
        this.chars = chars;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        CharSubscription sub = new CharSubscription(subscriber, executor, chars);
        sub.request(1);
    }

    static final class CharSubscription extends FlowGeneratorSubscription<Integer> {
        final CharSequence chars;

        int index;

        CharSubscription(Flow.Subscriber<? super Integer> actual, Executor executor, CharSequence chars) {
            super(actual, executor);
            this.chars = chars;
        }

        @Override
        public void run() {
            Flow.Subscriber<? super Integer> a = actual;

            if (!hasSubscribed) {
                hasSubscribed = true;
                a.onSubscribe(this);
                if (decrementAndGet() == 0) {
                    return;
                }
            }

            CharSequence q = chars;
            int f = q.length();
            long r = get();
            int idx = index;
            long e = 0L;

            for (;;) {

                while (e != r && idx != f) {
                    if (cancelled) {
                        return;
                    }
                    if (badRequest) {
                        cancelled = true;
                        a.onError(new IllegalArgumentException("§3.9 violated: request must be positive"));
                        return;
                    }

                    a.onNext((int)q.charAt(idx));

                    e++;
                    idx++;
                }

                if (idx == f) {
                    if (!cancelled) {
                        a.onComplete();
                    }
                    return;
                };

                r = get();
                if (e == r) {
                    index = idx;
                    r = addAndGet(-r);
                    if (r == 0L) {
                        break;
                    }
                    e = 0L;
                }
            }
        }
    }
}
