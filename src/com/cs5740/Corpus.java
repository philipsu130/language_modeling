package com.cs5740;

import com.cs5740.models.NgramModel;
import com.cs5740.tokenlist.LinkedTokenList;
import com.cs5740.tokenlist.TokenList;

import static com.cs5740.models.NgramModel.NgramModelBuilder;

import java.io.*;
import java.util.*;

/**
 * A class representing a corpus of texts, with unigram and bigram support.
 *
 * @author Kelvin Jin (kkj9), Philip Su (ps845)
 */
public class Corpus {
    static final String PATH_TO_BOOKS_TRAIN = "data/books/train_books/";
    static final String PATH_TO_BOOKS_TEST = "data/books/test_books/";
    // The regex string that matches punctuation that we want to turn into tokens.
    static final String PUNCTUATION = "([^a-zA-Z0-9' -])";

    // A random number generator. We only keep track of this to prevent non-deterministic outcomes.
    Random random = new Random(1);
    // A collection of all the tokens in this corpus stored as n-gram models.
    Map<Integer, NgramModel> ngramModels = new HashMap<>();
    // The name of this corpus
    final String name;

    private Corpus(final String name) {
        this.name = name;
        random.setSeed(0);
    }

//    public static Map<String, String> classifyGenre() {
//        String[] trainGenres = new File(PATH_TO_BOOKS_TRAIN).list();
//        String[] testGenres = new File(PATH_TO_BOOKS_TEST).list();
//        for (String testGenre : testGenres) {
//            double maxValue = 0.0;
//            String bestGenre = "";
//            for (String trainGenre : trainGenres) {
//                try {
//                    double value = Corpus.calculatePerplexityFromModel(trainGenre, testGenre, Ngram.UNIGRAM);
//                    if (maxValue < value) {
//                        bestGenre = trainGenre;
//                    }
//                } catch (IOException e) {
//                    System.out.println("Failed with train: " + trainGenre + ", test: " + testGenre + ": " + e.getMessage());
//                }
//            }
//        }
//        Map<String, String> classification = new HashMap<>();
//        return classification;
//    }

    /**
     * Calculates the perplexity of this model compared to a test corpus.
     * @param testCorpus The test corpus to test on.
     * @param n The n-value for determining which n-gram to use. Both corpuses must support this n-gram.
     * @return A number representing the perplexity between this corpus and a test corpus.
     */
    public double calculatePerplexityFromModel(final Corpus testCorpus, final int n) {
        double sum = 0.0;
        // Run through test set to find unknown tokens
        final NgramModel trainNgramModel = this.ngramModels.get(n);
        final NgramModel testNgramModel = testCorpus.ngramModels.get(n);
        if (trainNgramModel != null && testNgramModel != null) {
            final Iterator<TokenList> iterator = testNgramModel.getIterator();
            while (iterator.hasNext()) {
                final TokenList tokenList = iterator.next();
                // Compute the contribution to the sum
                int numOccurrences = testNgramModel.getUnsmoothedTokenFrequency(tokenList);
                double anomalyScore = Utils.getAnomalyScore(trainNgramModel.getProbability(tokenList, SmoothOptions.DEFAULT));
                sum += numOccurrences * anomalyScore;
            }
            sum /= testNgramModel.getTotalCount();
            sum = Math.exp(sum);
        }
        return sum;
    }

    /**
     * Creates a sentence that is numWords words long, using the given n-gram model.
     *
     * @param n        The n parameter to decide which n-gram model to use.
     * @param numWords The number of words that should be in the sentence.
     * @return A generated sentence.
     */
    public String createSentence(int n, int numWords) {
        if (numWords <= 0 || !ngramModels.containsKey(n)) {
            return "";
        }
        NgramModel model = ngramModels.get(n);
        TokenList previousWords = new LinkedTokenList();
        StringBuilder result = new StringBuilder(ngramModels.get(1).getWord(null, random.nextDouble()));
        previousWords.addLast(result.toString());
        numWords--;
        while (numWords > 0) {
            // Get the next word. This involves us keeping a list of previous words up-to-date.
            final String nextWord = generateNextWord(model, previousWords);
            if (n > 0) {
                previousWords.addLast(nextWord);
                if (previousWords.size() > n - 1) {
                    previousWords = previousWords.tail();
                }
            }
            addToStringBuilder(result, nextWord);
            numWords--;
        }
        return result.toString();
    }

    /**
     * Gets the name of this corpus.
     * @return The name of this corpus.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the probability that the given list of tokens is found in this corpus.
     *
     * @param tokens        The token list whose probability should be retrieved.
     * @param smoothOptions The smoothing option.
     * @return A probability value between 0 and 1.
     * If this corpus doesn't have an n-gram model for the given number of tokens,
     * zero is returned.
     */
    public double getProbability(TokenList tokens, SmoothOptions smoothOptions) {
        final int n = tokens.size();
        if (ngramModels.containsKey(n)) {
            return ngramModels.get(n).getProbability(tokens, smoothOptions);
        }
        return 0.0;
    }

    public NgramModel getNgramModel(int n) {
        if (ngramModels.containsKey(n)) {
            return ngramModels.get(n);
        } else {
            return null;
        }
    }

    //==========================================================================
    // Private helper methods
    //==========================================================================

    /**
     * Attempts to generate the next word in a model given a list of previous words.
     * If the list of previous words is too small, this method defaults to using the model with the largest n
     * for which the number of previous words is sufficient.
     *
     * @param model         The model to use when generating the next word.
     * @param previousWords The previous words to provide the context for the next word.
     * @return A randomly generated word.
     */
    private String generateNextWord(final NgramModel model, final TokenList previousWords) {
        NgramModel lModel = model;
        TokenList lPreviousWords = previousWords;
        if (lModel.getN() - 1 > lPreviousWords.size()) {
            int n = lPreviousWords.size() + 1;
            do {
                lModel = ngramModels.get(n);
                if (lModel != null && lModel.getN() == lPreviousWords.size()) {
                    lPreviousWords = lPreviousWords.tail();
                }
                n--;
            } while (lModel == null);
        }
        return lModel.getWord(lPreviousWords, random.nextDouble());
    }

    //==========================================================================
    // Private static helper methods
    //==========================================================================

    /**
     * Creates a corpus from an input stream.
     * @param name The name to give this corpus.
     * @param inputStream The input stream from which the corpus should be read.
     * @param maxN The maximum degree n-gram to generate.
     * @param unknownThreshold The maximum number of times a word should appear to be considered a rare word and not
     *                         part of the vocabulary.
     * @return A new corpus.
     */
    public static Corpus createCorpusFromInputStream(final String name, final InputStream inputStream, final int maxN, final int unknownThreshold) {
        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(inputStream);
        List<Integer> nList = new ArrayList<>();
        for (int i = 1; i <= maxN; i++) {
            nList.add(i);
        }
        Corpus c = new Corpus(name);
        c.ngramModels = createNgramModels(inputStreams, nList, unknownThreshold);
        return c;
    }

    /**
     * Creates a corpus from the given genre name.
     * Returns null if the genre name is not correctly specified.
     *
     * @param directory The directory in which the corpus texts are contained.
     * @param genreName The genre of books on which this corpus should be based.
     * @param maxN The maximum degree n-gram to generate.
     * @param unknownThreshold The maximum number of times a word should appear to be considered a rare word and not
     *                         part of the vocabulary.
     * @return A new Corpus object based on the given genre name.
     * @throws IOException If the folder corresponding to the given genre doesn't exist, or a file could not be opened.
     */
    public static Corpus createCorpusFromGenre(final String directory, final String genreName, final int maxN, final int unknownThreshold) throws IOException {
        return createCorpusFromGenre(directory, genreName, 1, maxN, unknownThreshold);
    }

    /**
     * Creates a corpus from the given genre name.
     * Returns null if the genre name is not correctly specified.
     *
     * @param directory The directory in which the corpus texts are contained.
     * @param genreName The genre of books on which this corpus should be based.
     * @param minN The minimum degree n-gram to generate.
     * @param maxN The maximum degree n-gram to generate.
     * @param unknownThreshold The maximum number of times a word should appear to be considered a rare word and not
     *                         part of the vocabulary.
     * @return A new Corpus object based on the given genre name.
     * @throws IOException If the folder corresponding to the given genre doesn't exist, or a file could not be opened.
     */
    public static Corpus createCorpusFromGenre(final String directory, final String genreName, final int minN, final int maxN, final int unknownThreshold) throws IOException {
        Corpus c = new Corpus(genreName);
        File genreDirectory = new File(directory + "/" + genreName + "/");
        List<Integer> nList = new ArrayList<>();
        for (int i = minN; i <= maxN; i++) {
            nList.add(i);
        }
        File[] filesInGenreDirectory = genreDirectory.listFiles();
        if (filesInGenreDirectory != null) {
            // Init input streams
            List<InputStream> inputStreams = new ArrayList<>();
            for (final File file : filesInGenreDirectory) {
                inputStreams.add(new FileInputStream(file));
            }
            c.ngramModels = Corpus.createNgramModels(inputStreams, nList, unknownThreshold);
            return c;
        }
        return null;
    }

    /**
     * Adds a token string to a StringBuilder object. A space is inserted only if the token
     * is not punctuation.
     *
     * @param stringBuilder The StringBuilder object to which the token should be appended.
     * @param token         The token to add.
     */
    private static void addToStringBuilder(StringBuilder stringBuilder, String token) {
        if (token.matches(PUNCTUATION)) {
            stringBuilder.append(token);
        } else {
            stringBuilder.append(" ").append(token);
        }
    }

    /**
     * Creates n-gram models from input streams for the n-values given.
     *
     * @param inputStreams The input streams from which the models should be created.
     * @param nList The n-values that should be used. These values should be unique and ideally consecutive.
     * @return A map of n-gram models. There should be an n-gram model for each n given.
     */
    private static Map<Integer, NgramModel> createNgramModels(final List<InputStream> inputStreams, final List<Integer> nList, int unknownThreshold) {
        final List<NgramModelBuilder> modelBuilders = new ArrayList<>();
        final List<TokenList> tokenLists = new ArrayList<>();
        final Map<String, Integer> wordCounts = new HashMap<>();
        // Get Max N
        int maxN = 0;
        for (final Integer n : nList) {
            maxN = n > maxN ? n : maxN;
            modelBuilders.add(NgramModel.getNgramModelBuilder(n));
            tokenLists.add(new LinkedTokenList());
        }
        BufferedReader bufferedReader;
        try {
            for (final InputStream inputStream : inputStreams) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = bufferedReader.readLine();
                while (line != null) {
                    // For each line, extract its tokens
                    String[] tokens = line.toLowerCase().replaceAll(PUNCTUATION, " $1 ").split("\\s");
                    for (String token : tokens) {
                        if (token.length() == 0) {
                            continue;
                        }
                        wordCounts.merge(token, 1, (a, b) -> a + b);
                        for (int i = 0; i < nList.size(); i++) {
                            // These are the running lists of n previously seen words
                            // (n determined by indexing nList[i])
                            tokenLists.get(i).addLast(token);
                            if (tokenLists.get(i).size() > nList.get(i)) {
                                tokenLists.set(i, tokenLists.get(i).tail());
                            }
                            // Use the list of n previously seen words, updated above,
                            // as the n-gram to add to the n-gram model
                            if (tokenLists.get(i).size() == nList.get(i)) {
                                modelBuilders.get(i).addTokens(tokenLists.get(i));
                            }
                        }
                    }
                    line = bufferedReader.readLine();
                }
            }
        } catch (IOException e) {
            return null;
        }
        Map<Integer, NgramModel> ngramModels = new HashMap<>();
        Set<String> rareWords = new HashSet<>();
        wordCounts.entrySet().forEach(e -> {
            if (e.getValue() <= unknownThreshold) {
                rareWords.add(e.getKey());
            }
        });
        for (final NgramModelBuilder modelBuilder : modelBuilders) {
            NgramModel model = modelBuilder.collapseRareWords(rareWords).build();
            ngramModels.put(model.getN(), model);
        }
        return ngramModels;
    }
}