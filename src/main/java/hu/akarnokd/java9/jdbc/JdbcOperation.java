package hu.akarnokd.java9.jdbc;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.function.*;

/**
 * Represents an immutable instruction to be executed by the database on a
 * specific JdbcConnection.
 * <p>
 *     JdbcOperation's are not expected to be interchangeable between connections or between
 *     driver vendors.
 * </p>
 */
public interface JdbcOperation {

    /**
     * Builder to set up the query and its parameters.
     */
    interface Builder {

        /**
         * Sets the query to be executed.
         * @param query the SQL query string
         * @return this Builder
         */
        Builder query(String query);

        /**
         * Indicates the execution of this query begins only when there has been
         * a subscription to the JdbcStatement.results() that is created for this
         * JdbcOperation.
         * @return this Builder
         */
        Builder deferred();

        /**
         * Indicates the execution of this query should start as soon as possible
         * and there is no need for subscribing to the JdbcStatement.results() that
         * is created for this JdbcOperation.
         * @return this Builder
         */
        Builder eager();

        /**
         * Set a value to a parameter.
         * @param id the parameter identifier
         * @param type the target parameter type
         * @param value the parameter value, null indicates nulling out that parameter
         * @return this Builder
         */
        Builder parameter(String id, JdbcDataType type, Object value);

        /**
         * Set a parameter from the sequence of byte arrays provided.
         * <ul>
         *     <li>The consumption of this Flow.Publisher happens only when this
         *     JdbcOperation executes.</li>
         *     <li>If the Flow.Publisher fails, the associated JdbcStatement will
         *     indicate this failure.</li>
         * </ul>
         * @param id the parameter identifier
         * @param bytes the Flow.Publisher providing the bytes in arrays.
         * @return this Builder
         */
        Builder parameterBytes(String id, Flow.Publisher<byte[]> bytes);

        /**
         * Sets a parameter to contain the sequence of bytes provided via a sequence
         * of ByteBuffers.
         * <ul>
         *     <li>The consumption of this Flow.Publisher happens only when this
         *     JdbcOperation executes.</li>
         *     <li>If the Flow.Publisher fails, the associated JdbcStatement will
         *     indicate this failure.</li>
         * </ul>
         * @param id the parameter identifier
         * @param buffers the Flow.Publisher providing the ByteBuffers
         * @return this Builder
         * @see #parameterBuffer(String, Flow.Publisher, Consumer)
         */
        Builder parameterBuffer(String id, Flow.Publisher<ByteBuffer> buffers);

        /**
         * Sets a parameter to contain the sequence of bytes provided via a sequence
         * of ByteBuffers and each spent ByteBuffer is then sent to an user-provided
         * Consumer.
         * <ul>
         *     <li>The consumption of this Flow.Publisher happens only when this
         *     JdbcOperation executes.</li>
         *     <li>If the Flow.Publisher fails, the associated JdbcStatement will
         *     indicate this failure.</li>
         *     <li>This allows to feed the JdbcOperation/JdbcStatement to be fed
         *     by pooled ByteBuffers that when fully consumed can be reused later.</li>
         * </ul>
         * @param id the parameter identifier
         * @param buffers the Flow.Publisher providing the ByteBuffers
         * @param release called with the ByteBuffer received through the Flow.Publisher
         * @return this Builder
         */
        Builder parameterBuffer(String id, Flow.Publisher<ByteBuffer> buffers, Consumer<ByteBuffer> release);

        /**
         * Clears all parameters.
         * @return this Builder
         */
        Builder clearParameters();

        /**
         * Removes the parameter set with the specified identifier.
         * @param id the parameter identifier
         * @return this Builder
         */
        Builder removeParameter(String id);

        /**
         * Construct an immutable JdbcOperation based on the values of this Builder.
         * @return the JdbcOperation with the specified parameters.
         */
        JdbcOperation build();

        /**
         * Consumes a Flow of values to be converted into a multi-valued JdbcOperation.
         * <ul>
         *     <li>Calling this method multiple times, similar to {@link #parameter(String, JdbcDataType, Object)}
         *     will replace any previous batch of parameters</li>
         * </ul>
         * @param values the values to be turned into parameters
         * @param build the consumer that receives the item from the values Flow.Publisher
         *              and a Builder on which the regular {@link #parameter(String, JdbcDataType, Object)}
         *              methods can be executed.
         *              calling {@link #query(String)}, {@link #build()} or {@link #batch(Flow.Publisher, BiConsumer)}
         *              on this Builder will result in an UnsupportedOperationException
         * @param <T> the value type to turn into parameters
         * @return this Builder
         */
        <T> Builder batch(Flow.Publisher<T> values, BiConsumer<? super T, Builder> build);
    }

    /**
     * Create a copy of this JdbcOperation.
     * @return a Builder prepared with parameters that were used to set up this JdbcOperation
     */
    Builder copy();

    /**
     * Indicates the Flow.Publisher returned by the associated JdbcStatement.results()
     * has to be subscribed to in order to begin the processing of this JdbcOperation.
     */
    boolean isDeferred();
}
