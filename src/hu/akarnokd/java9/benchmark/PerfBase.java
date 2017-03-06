package hu.akarnokd.java9.benchmark;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.Callable;

public abstract class PerfBase {
    static volatile Object o;

    static final VarHandle O;
    static {
        try {
            O = MethodHandles.lookup().findStaticVarHandle(SubmissionPublisherPerf.class, "o", Object.class);
        } catch (Throwable ex) {
            throw new InternalError(ex);
        }
    }
    static final int TIME_MILLIS = 1000;

    public static void benchmark(String name, Callable<?> call) {
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
}
