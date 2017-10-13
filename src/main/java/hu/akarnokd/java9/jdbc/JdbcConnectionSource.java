package hu.akarnokd.java9.jdbc;

import java.util.concurrent.Flow;

/**
 * Represents a way to establish connections to a database.
 */
public interface JdbcConnectionSource {

    /**
     * Prepare configuration for the connection provided by this
     * JdbcConnectionSource.
     */
    interface Builder {

        Builder url(String url);

        Builder username(String username);

        Builder password(String password);

        JdbcConnectionSource build();
    }

    /**
     * Returns a Flow.Publisher which when subscribed to
     * establishes a connection asynchronously to the database and
     * signals a single JdbcConnection instance representing
     * that connection.
     * <ul>
     *     <li>Cancelling the Subscription will cancel the connection attempt.</li>
     *     <li>The Flow.Publisher completes after it emits the connected JdbcConnection.</li>
     *     <li>Connection failures will be reported through onError.</li>
     * </ul>
     * @return the Flow.Publisher to initiate and receive a connection.
     */
    Flow.Publisher<JdbcConnection> connect();

    Builder copy();
}
