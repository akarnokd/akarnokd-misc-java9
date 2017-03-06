package hu.akarnokd.java9.benchmark;

import hu.akarnokd.java9.flow.FlowAPI;
import hu.akarnokd.java9.flow.FlowAPIPlugins;
import hu.akarnokd.java9.flow.functionals.FlowFunction;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public static void main(String[] args) {

        FlowAPIPlugins.executor = Runnable::run;

        benchmark("ShakespearePlaysScrabbleFlowOpt", () -> {

            return bench();
        });

        /*
        ExecutorService exec = Executors.newCachedThreadPool();
        FlowAPIPlugins.executor = exec;
        */

        FlowAPIPlugins.reset();

        benchmark("ShakespearePlaysScrabbleFlowOpt-Async", () -> {

            return bench();
        });

        //exec.shutdownNow();
    }
}
