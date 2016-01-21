package com.cs5740.models;


import com.cs5740.tokenlist.LinkedTokenList;
import com.cs5740.tokenlist.TokenList;

import java.util.*;

/**
 * A collection of tokens.
 * <p>
 * This class is implemented by maintaining a list of tokens, as well as a mapping from a token string to
 * its index in the list.
 */
class UnigramModel extends NgramModel {
    private List<TokenFrequencyObject> tokenFrequencies = new ArrayList<>();
    private Map<String, Integer> tokenIndexLookupTable = new HashMap<>();

    private UnigramModel() {}

    public String getWord(final TokenList previousTokens, final double p) {
        double prob = p;
        if (totalCount == 0 || prob < 0.0 || prob >= 1.0) {
            return "";
        }
        // Multiply p to scale it to the range [0.0, totalCount).
        prob *= totalCount;
        // Utilize binary search to find the word associated with the range in which p falls.
        // This range is determined by the cumulative frequency field in each token object.
        int left = 0;
        int right = tokenFrequencies.size() - 1;
        while (left <= right) {
            int mid = (left + right) / 2;
            TokenFrequencyObject element = tokenFrequencies.get(mid);
            if (prob >= element.cumulativeFrequency && prob < element.cumulativeFrequency + element.frequency) {
                return element.value;
            } else if (prob < element.cumulativeFrequency) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        // Should never hit this point
        return "";
    }

    @Override
    public int getUnsmoothedTokenFrequency(final TokenList tokens) {
        final String token = tokens.head().toLowerCase();
        if (tokenIndexLookupTable.containsKey(token)) {
            int tokenIndex = tokenIndexLookupTable.get(token);
            return tokenFrequencies.get(tokenIndex).frequency;
        } else if (!isInVocabulary(token) && tokenIndexLookupTable.containsKey(UNKNOWN_WORD_TOKEN)) {
            return tokenFrequencies.get(tokenIndexLookupTable.get(UNKNOWN_WORD_TOKEN)).frequency;
        }
        return 0;
    }

    @Override
    public int getN() {
        return 1;
    }

    @Override
    public Iterator<TokenList> getIterator() {
        return new Iterator<TokenList>() {
            final Iterator<TokenFrequencyObject> iterator = tokenFrequencies.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public TokenList next() {
                return new LinkedTokenList(iterator.next().value);
            }
        };
    }

    public static class UnigramModelBuilder implements NgramModelBuilder {
        Map<String, Integer> frequencyMap = new HashMap<>();

        public NgramModelBuilder addTokens(final TokenList tokens) {
            // Assume just one token
            final String token = tokens.head();
            if (frequencyMap.containsKey(token)) {
                int oldCount = frequencyMap.get(token);
                frequencyMap.put(token, oldCount + 1);
            } else {
                frequencyMap.put(token, 1);
            }
            return this;
        }

        @Override
        public NgramModelBuilder addTokensNumTimes(final TokenList tokens, final int num) {
            if (num == 0) {
                return this;
            }
            // Assume just one token
            final String token = tokens.head();
            if (frequencyMap.containsKey(token)) {
                int oldCount = frequencyMap.get(token);
                frequencyMap.put(token, oldCount + num);
            } else {
                frequencyMap.put(token, num);
            }
            return this;
        }

        @Override
        public NgramModelBuilder absorb(NgramModelBuilder other) {
            UnigramModelBuilder otherU = (UnigramModelBuilder)other;
            otherU.frequencyMap.entrySet().forEach(e -> {
                if (frequencyMap.containsKey(e.getKey())) {
                    frequencyMap.put(e.getKey(), frequencyMap.get(e.getKey()) + e.getValue());
                } else {
                    frequencyMap.put(e.getKey(), e.getValue());
                }
            });
            return this;
        }

        public NgramModelBuilder collapseRareWords(final Set<String> rareWords) {
            if (rareWords.size() == 0) {
                return this;
            }
            final Map<String, Integer> newMap = new HashMap<>();
            newMap.put(UNKNOWN_WORD_TOKEN, 0);
            frequencyMap.entrySet().forEach(e -> {
                if (rareWords.contains(e.getKey())) {
                    newMap.put(UNKNOWN_WORD_TOKEN, newMap.get(UNKNOWN_WORD_TOKEN) + e.getValue());
                } else {
                    newMap.put(e.getKey(), e.getValue());
                }
            });
            frequencyMap = newMap;
            return this;
        }

        public NgramModel build() {
            // Add unknown
            UnigramModel model = new UnigramModel();
            model.tokenFrequencies = new ArrayList<>();
            model.tokenIndexLookupTable = new HashMap<>();
            model.vocabulary = new HashSet<>();
            for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
                model.vocabulary.add(entry.getKey());
                model.tokenFrequencies.add(new TokenFrequencyObject(entry.getKey(), entry.getValue(), model.totalCount));
                model.tokenIndexLookupTable.put(entry.getKey(), model.tokenFrequencies.size() - 1);
                model.totalCount += entry.getValue();
                model.totalUniqueCount++;
            }
            model.frequencyCountMap = createTokenCountMap(frequencyMap);
            model.frequencyCountMap.put(0, 0);
            return model;
        }

        /**
         * Creates a mapping from frequency to the number n-grams with that frequency.
         * @param map The token frequency map.
         * @return map The token count mapping.
         */
        private static Map<Integer, Integer> createTokenCountMap(final Map<String, Integer> map) {
            Map<Integer, Integer> frequencyCountMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                int frequency = entry.getValue();
                if (frequencyCountMap.containsKey(frequency)) {
                    int oldFrequencyCount = frequencyCountMap.get(frequency);
                    frequencyCountMap.put(frequency, oldFrequencyCount + 1);
                } else {
                    frequencyCountMap.put(frequency, 1);
                }
            }
            frequencyCountMap.put(0,0);
            return frequencyCountMap;
        }
    }
}