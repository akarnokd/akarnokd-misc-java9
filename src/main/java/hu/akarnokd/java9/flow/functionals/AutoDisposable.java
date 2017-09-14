package hu.akarnokd.java9.flow.functionals;

public interface AutoDisposable extends AutoCloseable {
    @Override
    void close();
}
