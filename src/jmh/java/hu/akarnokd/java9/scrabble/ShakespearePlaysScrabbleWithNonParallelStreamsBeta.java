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

import java.util.stream.Stream;

/**
 * Shakespeare plays Scrabble with Java Streams (slightly modified).
 * @author José
 */
public class ShakespearePlaysScrabbleWithNonParallelStreamsBeta extends ShakespearePlaysScrabbleWithStreamsBeta {

    @Override
    Stream<String> buildShakerspeareWordsStream() {
        return shakespeareWords.stream() ;
    }

    public static void main(String[] args) throws Exception {
        ShakespearePlaysScrabbleWithNonParallelStreamsBeta s = new ShakespearePlaysScrabbleWithNonParallelStreamsBeta();
        s.init();
        System.out.println(s.measureThroughput());
    }
}