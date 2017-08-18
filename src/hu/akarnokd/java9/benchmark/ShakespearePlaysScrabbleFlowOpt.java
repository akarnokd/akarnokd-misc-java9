package hu.akarnokd.java9.benchmark;

import akka.actor.*;
import hu.akarnokd.java9.flow.FlowAPI;
import hu.akarnokd.java9.flow.FlowAPIPlugins;
import hu.akarnokd.java9.flow.functionals.FlowFunction;
import hu.akarnokd.rxjava2.math.MathFlowable;
import hu.akarnokd.rxjava2.string.StringFlowable;
import io.reactivex.*;
import io.reactivex.functions.Function;

import java.util.*;
import java.util.concurrent.Executor;

public class ShakespearePlaysScrabbleFlowOpt extends ShakespearePlaysScrabblePerfBase {

    static Object bench() throws Exception {
        //  to compute the score of a given word
        FlowFunction<Integer, Integer> scoreOfALetter = letter -> letterScores[letter - 'a'];

        // score of the same letters in a word
        FlowFunction<Map.Entry<Integer, MutableLong>, Integer> letterScore =
                entry ->
                        letterScores[entry.getKey() - 'a'] *
                                Integer.min(
                                        (int)entry.getValue().get(),
                                        scrabbleAvailableLetters[entry.getKey() - 'a']
                                )
                ;


        FlowFunction<String, FlowAPI<Integer>> toIntegerIx =
                string -> FlowAPI.characters(string);

        // Histogram of the letters in a given word
        FlowFunction<String, FlowAPI<HashMap<Integer, MutableLong>>> histoOfLetters =
                word -> toIntegerIx.apply(word)
                        .collect(
                                () -> new HashMap<>(),
                                (HashMap<Integer, MutableLong> map, Integer value) ->
                                {
                                    MutableLong newValue = map.get(value) ;
                                    if (newValue == null) {
                                        newValue = new MutableLong();
                                        map.put(value, newValue);
                                    }
                                    newValue.incAndSet();
                                }

                        ) ;

        // number of blanks for a given letter
        FlowFunction<Map.Entry<Integer, MutableLong>, Long> blank =
                entry ->
                        Long.max(
                                0L,
                                entry.getValue().get() -
                                        scrabbleAvailableLetters[entry.getKey() - 'a']
                        )
                ;

        // number of blanks for a given word
        FlowFunction<String, FlowAPI<Long>> nBlanks =
                word -> histoOfLetters.apply(word)
                        .flatMapIterable(map -> map.entrySet())
                        .map(blank)
                        .sumLong();


        // can a word be written with 2 blanks?
        FlowFunction<String, FlowAPI<Boolean>> checkBlanks =
                word -> nBlanks.apply(word)
                        .map(l -> l <= 2L) ;

        // score taking blanks into account letterScore1
        FlowFunction<String, FlowAPI<Integer>> score2 =
                word -> histoOfLetters.apply(word)
                        .flatMapIterable(map -> map.entrySet())
                        .map(letterScore)
                        .sumInt();

        // Placing the word on the board
        // Building the streams of first and last letters
        FlowFunction<String, FlowAPI<Integer>> first3 =
                word -> FlowAPI.characters(word).take(3) ;
        FlowFunction<String, FlowAPI<Integer>> last3 =
                word -> FlowAPI.characters(word).skip(3) ;


        // Stream to be maxed
        FlowFunction<String, FlowAPI<Integer>> toBeMaxed =
                word -> FlowAPI.concatArray(first3.apply(word), last3.apply(word))
                ;

        // Bonus for double letter
        FlowFunction<String, FlowAPI<Integer>> bonusForDoubleLetter =
                word -> toBeMaxed.apply(word)
                        .map(scoreOfALetter)
                        .maxInt();

        // score of the word put on the board
        FlowFunction<String, FlowAPI<Integer>> score3 =
                word ->
                        FlowAPI.concatArray(
                                score2.apply(word),
                                bonusForDoubleLetter.apply(word)
                        )
                                .sumInt().map(v -> 2 * v + (word.length() == 7 ? 50 : 0));

        FlowFunction<FlowFunction<String, FlowAPI<Integer>>, FlowAPI<TreeMap<Integer, List<String>>>> buildHistoOnScore =
                score -> FlowAPI.fromIterable(shakespeareWords)
                        .filter(scrabbleWords::contains)
                        //.filter(word -> checkBlanks.apply(word).blockingFirst())
                        .filterAsync(checkBlanks)
                        .mapAsync(score, (w, j) -> Map.entry(j, w))
                        .collect(
                                () -> new TreeMap<Integer, List<String>>(Comparator.reverseOrder()),
                                (TreeMap<Integer, List<String>> map, Map.Entry<Integer, String> word) -> {
                                    //Integer key = score.apply(word).blockingFirst() ;
                                    List<String> list = map.get(word.getKey()) ;
                                    if (list == null) {
                                        list = new ArrayList<>() ;
                                        map.put(word.getKey(), list) ;
                                    }
                                    list.add(word.getValue()) ;
                                }
                        ) ;

        // best key / value pairs
        List<Map.Entry<Integer, List<String>>> finalList2 =
                buildHistoOnScore.apply(score3)
                        .flatMapIterable(map -> map.entrySet())
                        .take(3)
                        .collect(
                                () -> new ArrayList<Map.Entry<Integer, List<String>>>(),
                                (list, entry) -> {
                                    list.add(entry) ;
                                }
                        )
                        .blockingFirst() ;

        //System.out.println(finalList2);

        return finalList2;
    }

    static Flowable<Integer> chars(String word) {
//        return Flowable.range(0, word.length()).map(i -> (int)word.charAt(i));
        return StringFlowable.characters(word);
    }


    static Object bench2() throws Exception {
        //  to compute the score of a given word
        Function<Integer, Integer> scoreOfALetter = letter -> letterScores[letter - 'a'];

        // score of the same letters in a word
        Function<Map.Entry<Integer, MutableLong>, Integer> letterScore =
                entry ->
                        letterScores[entry.getKey() - 'a'] *
                                Integer.min(
                                        (int)entry.getValue().get(),
                                        scrabbleAvailableLetters[entry.getKey() - 'a']
                                )
                ;


        Function<String, Flowable<Integer>> toIntegerFlowable =
                string -> chars(string);

        // Histogram of the letters in a given word
        Function<String, Single<HashMap<Integer, MutableLong>>> histoOfLetters =
                word -> toIntegerFlowable.apply(word)
                        .collect(
                                () -> new HashMap<>(),
                                (HashMap<Integer, MutableLong> map, Integer value) ->
                                {
                                    MutableLong newValue = map.get(value) ;
                                    if (newValue == null) {
                                        newValue = new MutableLong();
                                        map.put(value, newValue);
                                    }
                                    newValue.incAndSet();
                                }

                        ) ;

        // number of blanks for a given letter
        Function<Map.Entry<Integer, MutableLong>, Long> blank =
                entry ->
                        Long.max(
                                0L,
                                entry.getValue().get() -
                                        scrabbleAvailableLetters[entry.getKey() - 'a']
                        )
                ;

        // number of blanks for a given word
        Function<String, Flowable<Long>> nBlanks =
                word -> MathFlowable.sumLong(
                        histoOfLetters.apply(word).flattenAsFlowable(
                                map -> map.entrySet()
                        )
                                .map(blank)
                )
                ;


        // can a word be written with 2 blanks?
        Function<String, Flowable<Boolean>> checkBlanks =
                word -> nBlanks.apply(word)
                        .map(l -> l <= 2L) ;

        // score taking blanks into account letterScore1
        Function<String, Flowable<Integer>> score2 =
                word -> MathFlowable.sumInt(
                        histoOfLetters.apply(word).flattenAsFlowable(
                                map -> map.entrySet()
                        )
                                .map(letterScore)
                ) ;

        // Placing the word on the board
        // Building the streams of first and last letters
        Function<String, Flowable<Integer>> first3 =
                word -> chars(word).take(3) ;
        Function<String, Flowable<Integer>> last3 =
                word -> chars(word).skip(3) ;


        // Stream to be maxed
        Function<String, Flowable<Integer>> toBeMaxed =
                word -> Flowable.concat(first3.apply(word), last3.apply(word))
                ;

        // Bonus for double letter
        Function<String, Flowable<Integer>> bonusForDoubleLetter =
                word -> MathFlowable.max(toBeMaxed.apply(word)
                        .map(scoreOfALetter)
                ) ;

        // score of the word put on the board
        Function<String, Flowable<Integer>> score3 =
                word ->
//                MathFlowable.sumInt(Flowable.concat(
//                        score2.apply(word).map(v -> v * 2),
//                        bonusForDoubleLetter.apply(word).map(v -> v * 2),
//                        Flowable.just(word.length() == 7 ? 50 : 0)
//                  ));
                        MathFlowable.sumInt(Flowable.concat(
                                score2.apply(word),
                                bonusForDoubleLetter.apply(word)
                        )).map(v -> v * 2 + (word.length() == 7 ? 50 : 0))
//                new FlowableSumIntArray<Integer>(
//                        score2.apply(word),
//                        bonusForDoubleLetter.apply(word)
//                ).map(v -> 2 * v + (word.length() == 7 ? 50 : 0))
                ;

        Function<Function<String, Flowable<Integer>>, Single<TreeMap<Integer, List<String>>>> buildHistoOnScore =
                score -> Flowable.fromIterable(shakespeareWords)
                        .filter(scrabbleWords::contains)
                        .filter(word -> checkBlanks.apply(word).blockingFirst())
                        .collect(
                                () -> new TreeMap<Integer, List<String>>(Comparator.reverseOrder()),
                                (TreeMap<Integer, List<String>> map, String word) -> {
                                    Integer key = score.apply(word).blockingFirst() ;
                                    List<String> list = map.get(key) ;
                                    if (list == null) {
                                        list = new ArrayList<>() ;
                                        map.put(key, list) ;
                                    }
                                    list.add(word) ;
                                }
                        ) ;

        // best key / value pairs
        List<Map.Entry<Integer, List<String>>> finalList2 =
                buildHistoOnScore.apply(score3).flattenAsFlowable(
                        map -> map.entrySet()
                )
                        .take(3)
                        .collect(
                                () -> new ArrayList<Map.Entry<Integer, List<String>>>(),
                                (list, entry) -> {
                                    list.add(entry) ;
                                }
                        )
                        .blockingGet() ;


//        System.out.println(finalList2);

        return finalList2 ;
    }

    public static void main(String[] args) throws Exception {

        System.out.println(bench2());
        benchmark("ShakespearePlaysScrabbleFlowableOpt", () -> {

            return bench2();
        });


        FlowAPIPlugins.executor = Runnable::run;

        System.out.println(bench());
        benchmark("ShakespearePlaysScrabbleFlowOpt", () -> {

            return bench();
        });

        FlowAPIPlugins.reset();

        System.out.println(bench());
        benchmark("ShakespearePlaysScrabbleFlowOpt-Async", () -> {

            return bench();
        });

        ActorSystem actorSystem = ActorSystem.create();

        /*
        FlowAPIPlugins.onExecutor = e -> {
            ActorRef actor = actorSystem.actorOf(Props.create(ActorExecutor.class));
            return r -> actor.tell(r, ActorRef.noSender());
        };
        */

        int actorCount = Runtime.getRuntime().availableProcessors();
        ActorRef[] actors = new ActorRef[actorCount];
        for (int i = 0; i < actors.length; i++) {
            actors[i] = actorSystem.actorOf(Props.create(ActorExecutor.class));
        }

        FlowAPIPlugins.onExecutor = new java.util.function.Function<Executor, Executor>() {
            int i;
            @Override
            public Executor apply(Executor e) {
                return r -> actors[(i++) % actors.length].tell(r, ActorRef.noSender());
            }
        };

        System.out.println(bench());
        benchmark("ShakespearePlaysScrabbleFlowOpt-Actor", () -> {

            return bench();
        });


        actorSystem.terminate();
    }

    static final class ActorExecutor extends UntypedActor {

        @Override
        public void onReceive(Object message) throws Exception {
            Runnable r = (Runnable)message;

            r.run();
        }
    }
}
