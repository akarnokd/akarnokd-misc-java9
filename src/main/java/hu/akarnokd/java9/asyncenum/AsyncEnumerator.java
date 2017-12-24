package hu.akarnokd.java9.asyncenum;

import java.util.concurrent.CompletionStage;

public interface AsyncEnumerator<T> {

    CompletionStage<Boolean> moveNext();

    T current();
}
