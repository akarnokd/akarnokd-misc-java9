package hu.akarnokd.java9.jdbc;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * Represents a row of result data.
 */
public interface JdbcRow {

    /**
     * Returns the value of the column identified via a String.
     * <ul>
     *     <li>Retrieveing blob-typed column values may be rejected by the driver implementation</li>
     * </ul>
     * @param id the column identifier
     * @param type the expected value type of the column
     * @param <T> the value type of the column
     * @return the column's value, null-valued columns return null
     * @throws IllegalArgumentException if the column is missing, cannot be converted to the
     * target type or can't be retrieved in a non-blocking fashion.
     */
    <T> T get(String id, Class<T> type);

    /**
     * Returns a Stream of identifiers available for {@link #get(String, Class)}.
     * @return the Stream of column identifiers
     */
    Stream<String> identifiers();

    /**
     * Returns the contents of a column as a sequence of byte arrays.
     * <ul>
     *    <li>The returned Flow.Publisher may or may not allow multiple Subscribers.</li>
     *    <li>The size of each byte array is determined by the driver.</li>
     * </ul>
     * @param id the column identifier
     * @return the Flow.Publisher that will emit the contents as byte arrays
     * @throws IllegalArgumentException if the column is missing or the driver doesn't
     * support retrieving that column as a sequence of byte arrays.
     */
    Flow.Publisher<byte[]> getBytes(String id);

    /**
     * Returns the contents of a column as a sequence of ByteBuffers.
     * <ul>
     *    <li>The returned Flow.Publisher may or may not allow multiple Subscribers.</li>
     *    <li>The capacity of the emitted ByteBuffer is determined by the driver.</li>
     * </ul>
     * @param id the column identifier
     * @return the Flow.Publisher that will emit the contents as ByteBuffers
     * @throws IllegalArgumentException if the column is missing or the driver doesn't
     * support retrieving that column as a sequence of ByteBuffers.
     */
    Flow.Publisher<ByteBuffer> getBuffer(String id);

    /**
     * Returns the contents of a column as a sequence of ByteBuffers (or subclasses) provided by
     * an supplier function.
     * <ul>
     *    <li>The returned Flow.Publisher may or may not allow multiple Subscribers.</li>
     *    <li>The capacity shown to the bufferSupplier function is a hint for sizing the
     *    ByteBuffers returned. The driver should adapt to the actual capacity of each individual
     *    ByteBuffers returned by the function.</li>
     * </ul>
     * @param column the column identifier
     * @param bufferSupplier the function that receives a capacity value and should return a
     *                       ByteBuffer subclass that will be filled in.
     * @param <B> the ByteBuffer subclass
     * @return the Flow.Publisher that will emit the contents as custom ByteBuffers
     * @throws IllegalArgumentException if the column is missing or the driver doesn't
     * support retrieving that column as a sequence of ByteBuffers.
     */
    <B extends ByteBuffer> Flow.Publisher<B> getBuffer(int column, IntFunction<B> bufferSupplier);
}
