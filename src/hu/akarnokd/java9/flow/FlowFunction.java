package hu.akarnokd.java9.flow;

public interface FlowFunction<T, R> {

    R apply(T t) throws Exception;
}
