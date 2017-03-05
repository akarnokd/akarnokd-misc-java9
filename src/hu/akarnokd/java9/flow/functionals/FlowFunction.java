package hu.akarnokd.java9.flow.functionals;

public interface FlowFunction<T, R> {

    R apply(T t) throws Exception;
}
