package hu.akarnokd.java9.flow;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

public final class FlowAPIPlugins {

    private FlowAPIPlugins() {
        throw new IllegalStateException("No instances!");
    }

    public static volatile Function<FlowAPI, FlowAPI> onAssembly;

    public static volatile Function<Executor, Executor> onExecutor;

    public static volatile Executor executor = ForkJoinPool.commonPool();

    public static <T> FlowAPI<T> onAssembly(FlowAPI<T> source) {
        Function<FlowAPI, FlowAPI> onAssembly = FlowAPIPlugins.onAssembly;
        if (onAssembly == null) {
            return source;
        }
        return onAssembly.apply(source);
    }

    public static void reset() {
        onAssembly = null;
        onExecutor = null;
        executor = ForkJoinPool.commonPool();
    }

    public static Executor defaultExecutor() {
        Function<Executor, Executor> f = onExecutor;
        if (f != null) {
            return f.apply(executor);
        }
        return executor;
    }
}
