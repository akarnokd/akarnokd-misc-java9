package hu.akarnokd.java9;

import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompletableExceptions {

    //@Test
    public void test() {
        List<Throwable> error = new ArrayList<>();

        CompletableFuture<Integer> cf = new CompletableFuture<>();

        cf
                .whenComplete((v, e) -> error.add(e))
                .whenComplete((v, e) -> error.add(e))
                .thenCompose(v -> CompletableFuture.completedStage(1))
                .whenComplete((v, e) -> error.add(e));

        cf.completeExceptionally(new IOException("forced failure"));

        assertEquals(error.toString(), 3, error.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(i + ": " + error.get(i).toString(), error.get(i).getMessage().equals("forced failure"));
        }
    }

}
