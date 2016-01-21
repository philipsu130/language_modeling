package com.cs5740.models;

import com.cs5740.Utils;
import com.cs5740.tokenlist.LinkedTokenList;
import com.cs5740.tokenlist.TokenList;

import java.util.*;

/**
 * Represents an n-gram token collection.
 */
class MultigramModel extends NgramModel {
    Map<String, NgramModel> nMinusOneTokenCollectionMap = new HashMap<>();
    int n;

    public String getWord(final TokenList previousTokens, final double p) {
        final String token = previousTokens.head().toLowerCase();
        if (nMinusOneTokenCollectionMap.containsKey(token)) {
            return nMinusOneTokenCollectionMap.get(token).getWord(previousTokens.tail(), p);
        }
        return "";
    }

    @Override
    public int getUnsmoothedTokenFrequency(final TokenList tokens) {
        // Assume we got more than one token
        final String token = tokens.head().toLowerCase();
        if (nMinusOneTokenCollectionMap.containsKey(token)) {
            return nMinusOneTokenCollectionMap.get(token).getUnsmoothedTokenFrequency(tokens.tail());
        } else if (!isInVocabulary(token) && nMinusOneTokenCollectionMap.containsKey(UNKNOWN_WORD_TOKEN)) {
            return nMinusOneTokenCollectionMap.get(UNKNOWN_WORD_TOKEN).getUnsmoothedTokenFrequency(tokens.tail());
        }
        return 0;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public Iterator<TokenList> getIterator() {
        return new Iterator<TokenList>() {
            final Iterator<Map.Entry<String, NgramModel>> ngramIterator =
                    nMinusOneTokenCollectionMap.entrySet().iterator();
            Iterator<TokenList> tokenListIterator = null;
            int wordsLeft = totalUniqueCount;
            String token;

            @Override
            public boolean hasNext() {
                return wordsLeft > 0;
            }

            @Override
            public TokenList next() {
                if (!hasNext()) {
                    return null;
                }
                while (tokenListIterator == null || !tokenListIterator.hasNext()) {
                    if (ngramIterator.hasNext()) {
                        final Map.Entry<String, NgramModel> nextItem = ngramIterator.next();
                        tokenListIterator = nextItem.getValue().getIterator();
                        token = nextItem.getKey();
                    } else {
                        return null;
                    }
                }
                wordsLeft--;
                final TokenList tokenList = tokenListIterator.next();
                tokenList.addFirst(token);
                return tokenList;
            }
        };
    }

    public static class MultigramModelBuilder implements NgramModelBuilder {
        Map<String, NgramModelBuilder> frequencyMap = new HashMap<>();
        final int n;

        public MultigramModelBuilder(final int n) {
            this.n = n;
        }

        @Override
        public NgramModelBuilder collapseRareWords(final Set<String> rareWords) {
            if (rareWords.size() == 0) {
                return this;
            }
            frequencyMap.values().forEach(b -> b.collapseRareWords(rareWords));
            Map<String, NgramModelBuilder> newMap = new HashMap<>();
            newMap.put(UNKNOWN_WORD_TOKEN, NgramModel.getNgramModelBuilder(n - 1));
            frequencyMap.keySet().forEach(k -> {
                if (rareWords.contains(k)) {
                    newMap.get(UNKNOWN_WORD_TOKEN).absorb(frequencyMap.get(k));
                } else {
                    newMap.put(k, frequencyMap.get(k));
                }
            });
            frequencyMap = newMap;
            return this;
        }

        @Override
        public NgramModelBuilder addTokens(final TokenList tokens) {
            // Assume num tokens > 1
            final String token = tokens.head();
            if (!frequencyMap.containsKey(token)) {
                frequencyMap.put(token, NgramModel.getNgramModelBuilder(n - 1));
            }
            frequencyMap.get(token).addTokens(tokens.tail());
            return this;
        }

        @Override
        public NgramModelBuilder addTokensNumTimes(final TokenList tokens, final int num) {
            if (num == 0) {
                return this;
            }
            // Assume num tokens > 1
            final String token = tokens.head();
            if (!frequencyMap.containsKey(token)) {
                frequencyMap.put(token, NgramModel.getNgramModelBuilder(n - 1));
            }
            frequencyMap.get(token).addTokensNumTimes(tokens.tail(), num);
            return this;
        }

        @Override
        public NgramModelBuilder absorb(NgramModelBuilder other) {
            MultigramModelBuilder otherM = (MultigramModelBuilder)other;
            otherM.frequencyMap.entrySet().forEach(e -> {
                if (frequencyMap.containsKey(e.getKey())) {
                    frequencyMap.get(e.getKey()).absorb(e.getValue());
                } else {
                    frequencyMap.put(e.getKey(), e.getValue());
                }
            });
            return this;
        }

        @Override
        public NgramModel build() {
            MultigramModel model = new MultigramModel();
            model.n = n;
            model.frequencyCountMap = new HashMap<>();
            model.vocabulary = new HashSet<>();
            for (final String key : frequencyMap.keySet()) {
                NgramModel nMinusOneNgramModel = frequencyMap.get(key).build();
                model.vocabulary.addAll(nMinusOneNgramModel.vocabulary);
                model.totalCount += nMinusOneNgramModel.totalCount;
                model.totalUniqueCount += nMinusOneNgramModel.totalUniqueCount;
                for (final Integer key2 : nMinusOneNgramModel.frequencyCountMap.keySet()) {
                    if (model.frequencyCountMap.get(key2) != null) {
                        model.frequencyCountMap.put(key2,
                                model.frequencyCountMap.get(key2) + nMinusOneNgramModel.frequencyCountMap.get(key2));
                    } else {
                        model.frequencyCountMap.put(key2, nMinusOneNgramModel.frequencyCountMap.get(key2));
                    }
                }
                model.nMinusOneTokenCollectionMap.put(key, nMinusOneNgramModel);
            }
            int unseen = Utils.pow(model.getVocabularySize(), n);
            if (unseen < Integer.MAX_VALUE) {
                unseen -= model.getTotalUniqueCount();
            }
            model.frequencyCountMap.put(0, (int)unseen);
            return model;
        }
    }
}
