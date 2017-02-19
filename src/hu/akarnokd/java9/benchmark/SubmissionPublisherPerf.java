package hu.akarnokd.java9.benchmark;

import org.reactivestreams.Subscription;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.Callable;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionPublisherPerf {

    static volatile Object o;

    static final VarHandle O = checked(() -> MethodHandles.lookup().findStaticVarHandle(SubmissionPublisherPerf.class, "o", Object.class));

    volatile Object b;

    static final VarHandle B = checked(() -> MethodHandles.lookup().findVarHandle(SubmissionPublisherPerf.class, "b", Object.class));

    static <T> T checked(Callable<T> call) {
        try {
            return call.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static final int TIME_MILLIS = 1000;

    static void benchmark(String name, Callable<?> call) {
        try {
            System.out.print("# ");
            System.out.println(name);
            long[] timesCounts = new long[20];
            for (int i = 0; i < 10; i++) {
                System.out.printf("# Iteration %2d: ", i + 1);
                long start = System.currentTimeMillis();
                long end;
                long count = 0;
                while ((end = System.currentTimeMillis()) - start < TIME_MILLIS) {
                    O.setRelease(call.call());
                    count++;
                }
                timesCounts[i * 2] = end - start;
                timesCounts[i * 2 + 1] = count;

                System.out.printf("%,.3f ops/s%n", count * 1000d / (end - start));
            }

            System.out.print(name);
            System.out.print(" ");
            long sum = 0;
            long cnt = 0;

            for (int i = 5; i < 10; i++) {
                sum += timesCounts[i * 2];
                cnt += timesCounts[i * 2 + 1];
            }

            System.out.printf("%,.3f ops/s%n%n", cnt * 1000d / sum);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    static final class MixedSubscriber<T> implements Flow.Subscriber<T>, org.reactivestreams.Subscriber<T> {

        volatile Object item;
        static final VarHandle ITEM = checked(() -> MethodHandles.lookup().findVarHandle(MixedSubscriber.class, "item", Object.class));

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T item) {
            ITEM.setRelease(this, item);
        }

        @Override
        public void onError(Throwable throwable) {
            ITEM.setRelease(this, throwable);
        }

        @Override
        public void onComplete() {
            ITEM.setRelease(this, null);
        }

    }

    static <T> MixedSubscriber<T> newConsumer() {
        return new MixedSubscriber<T>();
    }

    public static void main(String[] args) {

        benchmark("SubmissionPublisher", () -> {
            MulticastPublisher<Integer> sp = new MulticastPublisher<>(Runnable::run, 128);

            MixedSubscriber<Integer> fs = newConsumer();
            sp.subscribe(fs);
            Integer v = 0;
            for (int i = 0; i < 1_000_000; i++) {
                sp.offer(v);
            }
            return fs;
        });

/*
        benchmark("Baseline", () -> null);


        benchmark("SubmissionPublisher", () -> {
            SubmissionPublisher<Integer> sp = new SubmissionPublisher<>(Runnable::run, 128);

            MixedSubscriber<Integer> fs = newConsumer();
            sp.subscribe(fs);
            Integer v = 0;
            for (int i = 0; i < 1_000_000; i++) {
                sp.submit(v);
            }
            return fs;
        });

        benchmark("SubmissionPublisher.offer", () -> {
            SubmissionPublisher<Integer> sp = new SubmissionPublisher<>(Runnable::run, 128);

            MixedSubscriber<Integer> fs = newConsumer();
            sp.subscribe(fs);
            Integer v = 0;
            for (int i = 0; i < 1_000_000; i++) {
                sp.offer(v, null);
            }
            return fs;
        });

        benchmark("PublishProcessor", () -> {
            PublishProcessor<Integer> ps = PublishProcessor.create();

            MixedSubscriber<Integer> fs = newConsumer();
            ps.strict().subscribe(fs);
            Integer v = 0;
            for (int i = 0; i < 1_000_000; i++) {
                ps.onNext(v);
            }
            return fs;
        });

        benchmark("PublishProcessor+observeOn", () -> {
            PublishProcessor<Integer> ps = PublishProcessor.create();

            MixedSubscriber<Integer> fs = newConsumer();
            ps.observeOn(ImmediateThinScheduler.INSTANCE).strict().subscribe(fs);
            Integer v = 0;
            for (int i = 0; i < 1_000_000; i++) {
                ps.onNext(v);
            }
            return fs;
        });

        benchmark("PublishProcessor-relaxed", () -> {
            PublishProcessor<Integer> ps = PublishProcessor.create();

            MixedSubscriber<Integer> fs = newConsumer();
            ps.subscribe(fs);
            Integer v = 0;
            for (int i = 0; i < 1_000_000; i++) {
                ps.onNext(v);
            }
            return fs;
        });

        benchmark("PublishProcessor-relaxed+observeOn", () -> {
            PublishProcessor<Integer> ps = PublishProcessor.create();

            MixedSubscriber<Integer> fs = newConsumer();
            ps.observeOn(ImmediateThinScheduler.INSTANCE).subscribe(fs);
            Integer v = 0;
            for (int i = 0; i < 1_000_000; i++) {
                ps.onNext(v);
            }
            return fs;
        });
        */
    }
}
