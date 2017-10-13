package hu.akarnokd.java9.jdbc;

import java.util.concurrent.Flow;
import java.util.function.BiConsumer;

/**
 * Represents an active connection to the database and allows
 * executing sequences of JdbcOperations on this connection.
 */
public interface JdbcConnection {

    /**
     * Prepares the execution JdbcOperations as a group provided by a Flow.Publisher.
     * <ul>
     *     <li>The group execution begins when the returned Flow.Publisher is subscribed to.</li>
     *     <li>Cancelling the Statement will prevent the execution of JdbcOperations
     *     that haven't been paired up with an emitted JdbcStatement. The already emitted
     *     JdbcStatements should be cancelled individually.</li>
     *     <li>This method is threadsafe and can be called multiple times.</li>
     *     <li>The Flow.Publisher completes after all JdbcStatements have been emitted
     *     and not when those JdbcStatements terminated individually.</li>
     *     <li>Drivers may limit the number of execute calls they are willing to manage at a
     *     given time. If there are too many execute() calls outstanding, the returned
     *     Flow.Publisher may simply fail with an onError.</li>
     *     <li>If the operations Flow.Publisher fails, the resulting Flow.Publisher will
     *     fail with the same error.</li>
     * </ul>
     * @param operations The sequence of operations. The instances of JdbcOperations
     *                   emitted by the Flow.Publisher should be instances created by
     *                   this JdbcConnection.
     * @return a Flow.Publisher which signals JdbcStatements that represent
     * the (future) execution of a JdbcOperation on a 1:1 basis and in order
     * with the input JdbcOperations.
     */
    Flow.Publisher<JdbcStatement> execute(Flow.Publisher<JdbcOperation> operations);

    /**
     * Closes the connection to the database; all outstanding JdbcStatement executions
     * and all query result processing will fail with an onError.
     * <ul>
     *     <li>The closing of the connection is triggered by the method call. The returned
     *     Flow.Publisher need not to be subscribed.</li>
     * </ul>
     * @return a Flow.Publisher which completes when the connection has been completely closed.
     */
    Flow.Publisher<Void> close();

    /**
     * Creates a JdbcOperation that indicates a transaction is to be started
     * and all subsequent JdbcOperations are part of that transaction until
     * the operations sequence ends or one of the {@link #commitTransaction()}
     * or {@link #rollbackTransaction()} is encountered.
     * @return the JdbcOperation
     */
    JdbcOperation beginTransaction();

    /**
     * Creates a JdbcOperation that indicates the current transaction to be ended.
     * The subsequent JdbcOperations will be executed outside a transaction until
     * the next {@link #beginTransaction()} JdbcOperation is encountered.
     * @return the JDBC operation
     */
    JdbcOperation endTransaction();

    /**
     * Creates a JdbcOperation that commits the current transaction started by
     * {@link #beginTransaction()}
     * @return the JdbcOperation
     */
    JdbcOperation commitTransaction();

    /**
     * Creates a JdbcOperation that rolls back the current transaction
     * since the {@link #beginTransaction()} or last {@link #commitTransaction()}.
     * @return the JdbcOperation
     */
    JdbcOperation rollbackTransaction();

    /**
     * Creates a JdbcOperation that indicates the subsequent JdbcOperations are
     * to be executed one after the other. This execution mode ends
     * at the next {@link #beginParallel()} JdbcOperation or when the
     * operations Flow.Publisher completes.
     * @return the JdbcOperation
     */
    JdbcOperation beginSequential();

    /**
     * Creates a JdbcOperation that indicates the subsequent JdbcOperations are to
     * be executed in parallel with each other. This execution mode ends
     * at the next {@link #beginSequential()} or when the operations Flow.Publisher
     * completes.
     * @return the JdbcOperation
     */
    JdbcOperation beginParallel();

    /**
     * Creates a JdbcOperation that indicates when any the subsequent JdbcOperations
     * encounter an error the processing of the JdbcOperation sequence is stopped
     * (as if cancel() was called on the JdbcStatement Flow.Publisher).
     * This execution mode ends at the next {@link #onErrorResumeNext()} JdbcOperation
     * or when the operations Flow.Publisher completes.
     * Ongoing JdbcStatement processing may be allowed to complete normally or they
     * can result in an onError, depending on the driver's decision.
     * @return the JdbcOperation
     */
    JdbcOperation onErrorStop();

    /**
     * Creates a JdbcOperation that indicates if any of the subsequeent JdbcOperations
     * fail, the remaining JdbcOperations can be still executed.
     * This execution mode ends at the next {@link #onErrorStop()} JdbcOperation
     * or when the operations Flow.Publisher completes.
     * @return the JdbcOperation
     */
    JdbcOperation onErrorResumeNext();

    /**
     * Creates a new query builder that allows building up (parameterized) SQL statements.
     * @return the JdbcOperation.Builder instance
     */
    JdbcOperation.Builder query();
}
