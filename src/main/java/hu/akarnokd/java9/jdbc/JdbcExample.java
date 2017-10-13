package hu.akarnokd.java9.jdbc;

import hu.akarnokd.reactive4javaflow.*;

public class JdbcExample {

    static JdbcConnectionSource connectionSource() {
        return null;
    }

    public static void main(String[] args) {
        Esetleg.fromPublisher(connectionSource().connect())
                .observeOn(SchedulerServices.computation())
                .flatMapPublisher(conn -> {
                    return Folyam.fromPublisher(conn.execute(
                            Esetleg.fromCallable(() -> {
                                return conn.query()
                                        .query("SELECT :v FROM DUAL")
                                        .parameter("v", JdbcDataType.INT, 1)
                                        .build();
                            })
                    ))
                    .flatMap(stmt -> {
                        return Folyam.fromPublisher(
                                    stmt.results(row -> row.get("1", Integer.TYPE))
                                ).observeOn(SchedulerServices.single());
                    })
                    .concatWith(Folyam.fromPublisher(conn.close()).map(v -> 0));
                })
                .blockingSubscribe(System.out::println, Throwable::printStackTrace);
    }
}
