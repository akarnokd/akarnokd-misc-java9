package hu.akarnokd.java9.flow.functionals;

public interface FlowFunction2<T, U, R> {

    R apply(T t, U u) throws Exception;
}
