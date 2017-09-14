package hu.akarnokd.java9.flow.functionals;

public interface FlowConsumer2<T, U> {

    void accept(T t, U u) throws Exception;
}
