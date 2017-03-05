package hu.akarnokd.java9.flow.functionals;

public interface FlowConsumer<T> {

    void accept(T t) throws Exception;
}
