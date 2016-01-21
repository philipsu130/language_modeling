package com.cs5740.models;

import com.cs5740.Corpus;
import com.cs5740.SmoothOptions;
import com.cs5740.tokenlist.LinkedTokenList;
import com.cs5740.tokenlist.TokenList;

import java.util.*;

/**
 * Represents an n-gram model.
 */
public abstract class NgramModel {
    int totalCount = 0;
    int totalUniqueCount = 0;
    Map<Integer, Integer> frequencyCountMap;
    Set<String> vocabulary;
    public static final String UNKNOWN_WORD_TOKEN = "<unk>";

    /**
     * Gets the probability with which the given tokens appear in this token collection.
     *
     * @param tokens The token list whose probability we should return.
     * @return The probability with which these tokens appear, as a number between 0.0 and 1.0.
     * If the token doesn't exist, this function returns zero.
     */
    public double getProbability(final TokenList tokens, final SmoothOptions smoothOptions) {
        double result = getTokenFrequency(tokens, smoothOptions) / (double)totalCount;
        if (result < 0) {
            return getTokenFrequency(tokens, smoothOptions);
        }
        return result;
    }

    /**
     * Returns a word determined by the given number.
     * <p>
     * Each word is assigned a range within the interval [0.0, 1.0), proportional to its frequency in this
     * token collection. This function simply returns the word with the range in which p falls.
     *
     * @param p A number in the range of [0.0, 1.0). This number should ideally be randomly generated.
     * @return The word determined by the given number. An empty string is returned if p falls outside
     * of the required range, or there are no tokens in this collection.
     */
    public abstract String getWord(final TokenList previousTokens, final double p);

    /**
     * Returns the frequency with which a list of tokens appears in the corpus, with
     * the given smoothing option.
     * @param tokens The tokens to find.
     * @return The frequency of the given list of tokens.
     */
    public double getTokenFrequency(final TokenList tokens, final SmoothOptions smoothOptions) {
        int tokenFrequency = getUnsmoothedTokenFrequency(tokens);
        if (tokenFrequency >= smoothOptions.getCutoff()) {
            // Use unsmoothed
            return tokenFrequency;
        } else {
            // Use smoothed
            if (frequencyCountMap.get(tokenFrequency + 1) == null) {
                return tokenFrequency;
            }
            int frequencyCount = frequencyCountMap.get(tokenFrequency);
            int oneLargerFrequencyCount = frequencyCountMap.get(tokenFrequency + 1);
            return (tokenFrequency + 1) * (oneLargerFrequencyCount / (double)frequencyCount);
        }
    }

    /**
     * Gets an n-gram's unsmoothed frequency in a corpus.
     * @param tokens The tokens to find.
     * @return The unsmoothed frequency of the given list of tokens.
     */
    public abstract int getUnsmoothedTokenFrequency(final TokenList tokens);

    /**
     * Gets the total number of n-grams in this model.
     * @return The total number of n-grams in this model.
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Gets the total number of unique n-grams in this model.
     * @return The total number of unique n-grams in this model.
     */
    public int getTotalUniqueCount() {
        return totalUniqueCount;
    }

    /**
     * Returns whether a single word type is in this model's vocabulary.
     * @param token The word type to test.
     * @return Whether a single word type is in this model's vocabulary.
     */
    public boolean isInVocabulary(final String token) {
        return vocabulary.contains(token);
    }

    /**
     * Gets the total number of unique words in this model.
     * @return The total number of unique words in this model.
     */
    public int getVocabularySize() {
        return vocabulary.size();
    }

    /**
     * Turns every unknown word into the unknown token, in a list.
     * @param tokenList A list of tokens. This object is not modified.
     * @return A new list of tokens.
     */
    public TokenList replaceUnknowns(final TokenList tokenList) {
        if (tokenList.containsUnknown()) {
            return tokenList;
        }
        TokenList modifiedTokenList = tokenList;
        final TokenList result = new LinkedTokenList();
        for (int i = 0; i < tokenList.size(); i++) {
            result.addLast(isInVocabulary(modifiedTokenList.head()) ? modifiedTokenList.head() : UNKNOWN_WORD_TOKEN);
            modifiedTokenList = modifiedTokenList.tail();
        }
        return result;
    }

    /**
     * Gets the n parameter for this n-gram model.
     * @return The value of n.
     */
    public abstract int getN();

    /**
     * Creates a new builder from this n-gram model.
     * @return A builder pre-initialized with the information contained in this n-gram model.
     */
    public NgramModelBuilder deconstruct() {
        final NgramModelBuilder builder = getNgramModelBuilder(getN());
        final Iterator<TokenList> iterator = getIterator();
        while (iterator.hasNext()) {
            final TokenList tokenList = iterator.next();
            builder.addTokensNumTimes(tokenList, getUnsmoothedTokenFrequency(tokenList));
        }
        return builder;
    }

    /**
     * Gets an iterator object that iterates through every unique n-gram in this model.
     * @return An iterator object.
     */
    public abstract Iterator<TokenList> getIterator();

    static class TokenFrequencyObject {
        public String value;
        public int frequency;
        public int cumulativeFrequency;

        /**
         * Constructs a new instance of the TokenFrequencyObject class.
         *
         * @param value               The string value associated with this token.
         * @param frequency           The frequency at which this token appears in the corpus.
         * @param cumulativeFrequency The sum of all token frequencies to the left of this token
         *                            in the corpus's list of tokens.
         */
        public TokenFrequencyObject(String value, int frequency, int cumulativeFrequency) {
            this.value = value;
            this.frequency = frequency;
            this.cumulativeFrequency = cumulativeFrequency;
        }
    }

    /**
     * An abstract class for n-gram model object builders.
     */
    public interface NgramModelBuilder {
        /**
         * Turns every occurrence of a word in the input set into the unknown word token, and
         * refactors accordingly.
         * @param rareWords The set of words the turn into unknown words.
         * @return A reference to this object.
         */
        NgramModelBuilder collapseRareWords(final Set<String> rareWords);

        /**
         * Adds an n-gram to this builder.
         * @param tokens A single n-gram to add.
         * @return A reference to this object.
         */
        NgramModelBuilder addTokens(final TokenList tokens);

        /**
         * Efficiently adds an n-gram to this builder a given number of times.
         * @param tokens A single n-gram to add.
         * @param num The number of times it should be added.
         * @return A reference to this object.
         */
        NgramModelBuilder addTokensNumTimes(final TokenList tokens, final int num);

        /**
         * Merges the information in the givne n-gram model builder into this one.
         * The n-values between this and other must be the same.
         * @param other The n-gram model builder to merge into this one.
         * @return A reference to this object.
         */
        NgramModelBuilder absorb(final NgramModelBuilder other);

        /**
         * Builds a new n-gram model from this object.
         * @return A new n-gram model object.
         */
        NgramModel build();
    }

    /**
     * Returns a builder object that will build the appropriate n-gram model, given n.
     * @param n The parameter for the n-gram model.
     * @return A builder object that may be used to build an n-gram model.
     */
    public static NgramModelBuilder getNgramModelBuilder(final int n) {
        if (n == 1) {
            return new UnigramModel.UnigramModelBuilder();
        } else {
            return new MultigramModel.MultigramModelBuilder(n);
        }
    }

    /**
     * Gets the human-readable name of an n-gram model.
     * @param n The n parameter for the n-gram model.
     * @return A human-readable name.
     */
    public static String getNgramName(final int n) {
        switch (n) {
            case 1:
                return "unigram";
            case 2:
                return "bigram";
            case 3:
                return "trigram";
            default:
                return n + "-gram";
        }
    }
}
