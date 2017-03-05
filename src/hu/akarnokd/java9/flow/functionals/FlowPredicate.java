package hu.akarnokd.java9.flow.functionals;

public interface FlowPredicate<T> {
    boolean test(T t) throws Exception;
}
