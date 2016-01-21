package com.cs5740;

import com.cs5740.models.NgramModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.function.Consumer;

public class Main {
    public static class RandomDigitInputSteam extends InputStream {
        private int numbersLeft;
        private Random random;
        boolean nextNumber;

        public RandomDigitInputSteam(int numbers, int seed) {
            random = new Random(seed);
            numbersLeft = numbers;
            nextNumber = false;
        }

        @Override
        public int read() throws IOException {
            if (numbersLeft <= 0) {
                return -1;
            }
            nextNumber = !nextNumber;
            if (nextNumber) {
                numbersLeft--;
                return '0' + random.nextInt(10);
            } else {
                return ' ';
            }
        }
    }

    final static int numWords = 300;
    final static int n = 3;
    final static int unknownThreshold = 2;
    final static String[] genreNames = new String[] { "children", "crime", "history" };

    final static Consumer<Corpus> displayRandomSentences = corpus -> {
        for (int i = 1; i <= n; i++) {
            final String ngramName = NgramModel.getNgramName(i);
            System.out.println("> " + numWords + "-word random sentence from " +
                    corpus.getName() + " corpus using " + ngramName + ":");
            System.out.println(corpus.createSentence(i, numWords));
        }
    };

    final static Consumer<Corpus> displayPerplexity = corpus -> {
        try {
            for (final String testGenreName : genreNames) {
                double perplexity = corpus.calculatePerplexityFromModel(
                        Corpus.createCorpusFromGenre(Corpus.PATH_TO_BOOKS_TEST, testGenreName, n, n, 0), n);
                System.out.println("Train: " + corpus.getName() + " | Test: " + testGenreName +
                        " | N: " + n + " | Perplexity: " + perplexity);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    };

    private static void processRandomDigitCorpuses() {
        final int numWords = 10000;
        final int n = 5;
        final Corpus c1 = Corpus.createCorpusFromInputStream("digits", new RandomDigitInputSteam(numWords, 0), n, 1);
        final Corpus c2 = Corpus.createCorpusFromInputStream("digits", new RandomDigitInputSteam(numWords, 0), n, 1);
        double perplexity = c1.calculatePerplexityFromModel(c2, n);
        System.out.println("N: " + n + " | Perplexity: " + perplexity);
    }

    @SafeVarargs
    private static void processGenreCorpuses(Consumer<Corpus>... actions) {
        try {
            for (final String genreName : genreNames) {
                long time = System.nanoTime();
                System.out.print("> Loading corpus " + genreName + "... ");
                final Corpus corpus = Corpus.createCorpusFromGenre(Corpus.PATH_TO_BOOKS_TRAIN, genreName, n, unknownThreshold);
                System.out.println("loaded in " + ((System.nanoTime() - time) / 1000000000.0) + " seconds.");
                for (final Consumer<Corpus> action : actions) {
                    action.accept(corpus);
                }
            }
        } catch (IOException e) {
            System.out.println("Yeah so something went wrong: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        processGenreCorpuses(displayPerplexity);
//        processRandomDigitCorpuses();
    }
}
