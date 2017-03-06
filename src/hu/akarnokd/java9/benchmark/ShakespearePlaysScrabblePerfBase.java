package hu.akarnokd.java9.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

public abstract class ShakespearePlaysScrabblePerfBase extends PerfBase {

    public static final class MutableLong {
        long value;
        long get() {
            return value;
        }

        MutableLong set(long l) {
            value = l;
            return this;
        }

        MutableLong incAndSet() {
            value++;
            return this;
        }

        MutableLong add(MutableLong other) {
            value += other.value;
            return this;
        }
    }

    public static final HashSet<String> shakespeareWords;

    public static final HashSet<String> scrabbleWords;

    public static final int [] letterScores = {
            // a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p,  q, r, s, t, u, v, w, x, y,  z
            1, 3, 3, 2, 1, 4, 2, 4, 1, 8, 5, 1, 3, 1, 1, 3, 10, 1, 1, 1, 1, 4, 4, 8, 4, 10} ;

    public static final int [] scrabbleAvailableLetters = {
            // a, b, c, d,  e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z
            9, 2, 2, 1, 12, 2, 3, 2, 9, 1, 1, 4, 2, 6, 8, 2, 1, 6, 4, 6, 4, 2, 2, 1, 2, 1} ;

    static {
        shakespeareWords = new HashSet<>();
        scrabbleWords = new HashSet<>();

        try {
            Files.lines(Paths.get("files/ospd.txt"))
                    .map(String::toLowerCase)
                    .forEach(scrabbleWords::add);

            Files.lines(Paths.get("files/shakespeareWords.shakespeare.txt"))
                    .map(String::toLowerCase)
                    .forEach(shakespeareWords::add);
        } catch (IOException ex) {
            throw new InternalError(ex);
        }
    }
}
