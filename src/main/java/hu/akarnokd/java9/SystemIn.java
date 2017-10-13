package hu.akarnokd.java9;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class SystemIn {
    public static void main( String[] args ) throws IOException {
        Observable.fromCallable( () -> getMsg() )
                .subscribeOn(Schedulers.io())
                .timeout( 5, TimeUnit.SECONDS )
                .blockingSubscribe( System.out::println,
                        e -> e.printStackTrace() );
        System.out.println( "End..." );
    }

    private static String getMsg() throws IOException {
        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );
        System.out.print( "Enter a msg:" );
        String msg = reader.readLine();
        return msg;
    }
}
