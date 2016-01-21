package com.cs5740.tokenlist;

/**
 * Just a list of tokens.
 *
 * Implementations of this class make no guarantee on that separate instances will not have pointers
 * to the same underlying information.
 */
public interface TokenList {
    void addFirst(final String element);

    void addLast(final String element);

    String head();

    int size();

    /**
     * Returns a token list representing every element except the first one.
     * @return A token list with every element in this one except the first.
     */
    TokenList tail();

    boolean containsUnknown();
}
