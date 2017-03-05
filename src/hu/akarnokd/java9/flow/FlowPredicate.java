package hu.akarnokd.java9.flow;

public interface FlowPredicate<T> {
    boolean test(T t) throws Exception;
}
