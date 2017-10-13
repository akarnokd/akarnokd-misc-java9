package hu.akarnokd.java9.jdbc;

import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * Represents an active JdbcOperation execution.
 */
public interface JdbcStatement {

    /**
     * The JdbcOperation for which this JdbcStatement has been created.
     * @return the JdbcOperation for which this JdbcStatement has been created
     */
    JdbcOperation operation();

    /**
     * Allows the transformation of the response rows into user-specific datatypes.
     * Depending on the JdbcOperation.isDeferred, subscribing to this Flow.Publisher
     * may be necessary to trigger the underlying processing.
     * <ul>
     *     <li>If there are no results, the Flow.Publisher completes without items.</li>
     *     <li>The returned Flow.Publisher may or may not allow multiple Subscribers.</li>
     *     <li>Implementations may chose to reuse the same JdbcRow instance when presenting
     *     newer rows.</li>
     *     <li>The returned Flow.Publisher terminates if all outstanding Flow.Publisher-based
     *     row data retrievals have terminated or have been cancelled.</li>
     * </ul>
     * @param rowTransformer the function called when a full row from the underlying query
     *                       becomes available for consumption and should return an user
     *                       specified instance to be emitted by the returned Flow.Publisher.
     *                       The JdbcRow instance should not escape the function.
     * @param <T> the value type emitted
     * @return the shared Flow.Publisher that emits the user-transformed result rows
     */
    <T> Flow.Publisher<T> results(Function<JdbcRow, T> rowTransformer);

    /**
     * Completes or fails if the statement has been fully consumed or failed for some reason.
     * @return the shared Flow.Publisher instance indicating the termination
     */
    Flow.Publisher<Void> completion();

    /**
     * Tries to prevent or cancel the execution of this JdbcStatement.
     * @return the shared Flow.Publisher instance indicating the success of cancellation
     */
    Flow.Publisher<Void> cancel();
}
