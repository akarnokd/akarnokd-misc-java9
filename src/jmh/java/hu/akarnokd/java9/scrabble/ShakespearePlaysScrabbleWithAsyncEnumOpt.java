/*
 * Copyright (C) 2015 José Paumard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package hu.akarnokd.java9.scrabble;

import hu.akarnokd.asyncenum.AsyncEnumerable;
import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Shakespeare plays Scrabble with Async Enumerables optimized.
 * @author José
 * @author akarnokd
 */
public class ShakespearePlaysScrabbleWithAsyncEnumOpt extends ShakespearePlaysScrabble {

    static AsyncEnumerable<Integer> chars(String word) {
        return AsyncEnumerable.characters(word);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(
        iterations = 5
    )
    @Measurement(
        iterations = 5
    )
    @Fork(1)
    public List<Entry<Integer, List<String>>> measureThroughput() throws InterruptedException {

        //  to compute the score of a given word
        Function<Integer, Integer> scoreOfALetter = letter -> letterScores[letter - 'a'];

        // score of the same letters in a word
        Function<Entry<Integer, MutableLong>, Integer> letterScore =
                entry ->
                        letterScores[entry.getKey() - 'a'] *
                        Integer.min(
                                (int)entry.getValue().get(),
                                scrabbleAvailableLetters[entry.getKey() - 'a']
                            )
                    ;


        Function<String, AsyncEnumerable<Integer>> toIntegerIx =
                string -> chars(string);

        // Histogram of the letters in a given word
        Function<String, AsyncEnumerable<HashMap<Integer, MutableLong>>> histoOfLetters =
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
        Function<Entry<Integer, MutableLong>, Long> blank =
                entry ->
                        Long.max(
                            0L,
                            entry.getValue().get() -
                            scrabbleAvailableLetters[entry.getKey() - 'a']
                        )
                    ;

        // number of blanks for a given word
        Function<String, AsyncEnumerable<Long>> nBlanks =
                word -> histoOfLetters.apply(word)
                            .flatMap(map -> AsyncEnumerable.fromIterable(map.entrySet()))
                            .map(blank)
                            .sumLong(v -> v);


        // can a word be written with 2 blanks?
        Function<String, AsyncEnumerable<Boolean>> checkBlanks =
                word -> nBlanks.apply(word)
                            .map(l -> l <= 2L) ;

        // score taking blanks into account letterScore1
        Function<String, AsyncEnumerable<Integer>> score2 =
                word -> histoOfLetters.apply(word)
                            .flatMap(map -> AsyncEnumerable.fromIterable(map.entrySet()))
                            .map(letterScore)
                            .sumInt(v -> v);

        // Placing the word on the board
        // Building the streams of first and last letters
        Function<String, AsyncEnumerable<Integer>> first3 =
                word -> chars(word).take(3) ;
        Function<String, AsyncEnumerable<Integer>> last3 =
                word -> chars(word).skip(3) ;


        // Stream to be maxed
        Function<String, AsyncEnumerable<Integer>> toBeMaxed =
            word -> AsyncEnumerable.concatArray(first3.apply(word), last3.apply(word))
            ;

        // Bonus for double letter
        Function<String, AsyncEnumerable<Integer>> bonusForDoubleLetter =
            word -> toBeMaxed.apply(word)
                        .map(scoreOfALetter)
                        .max(Comparator.naturalOrder());

        // score of the word put on the board
        Function<String, AsyncEnumerable<Integer>> score3 =
            word ->
                AsyncEnumerable.concatArray(
                        score2.apply(word),
                        bonusForDoubleLetter.apply(word)
                )
                .sumInt(v -> v).map(v -> 2 * v + (word.length() == 7 ? 50 : 0));

        Function<Function<String, AsyncEnumerable<Integer>>, AsyncEnumerable<TreeMap<Integer, List<String>>>> buildHistoOnScore =
                score -> AsyncEnumerable.fromIterable(shakespeareWords)
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
        List<Entry<Integer, List<String>>> finalList2 =
                buildHistoOnScore.apply(score3)
                    .flatMap(map -> AsyncEnumerable.fromIterable(map.entrySet()))
                    .take(3)
                    .collect(
                        () -> new ArrayList<Entry<Integer, List<String>>>(),
                        (list, entry) -> {
                            list.add(entry) ;
                        }
                    )
                    .blockingFirst() ;

//        System.out.println(finalList2);

        return finalList2 ;
    }

    public static void main(String[] args) throws Exception {
        ShakespearePlaysScrabbleWithAsyncEnumOpt s = new ShakespearePlaysScrabbleWithAsyncEnumOpt();
        s.init();
        System.out.println(s.measureThroughput());
    }
}