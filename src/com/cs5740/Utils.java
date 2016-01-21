package com.cs5740;

/**
 * Just some utility functions
 */
public class Utils {
    private Utils() {}

    /**
     * Returns the negative natural log of the given number.
     * @param probability A probability value, between 0 and 1.
     * @return The negative natural log of the given number.
     */
    public static double getAnomalyScore(double probability) {
        return -Math.log(probability);
    }

    /**
     * Returns min(Integer.MAX_VALUE, n ^ x).
     * @param n The number at the bottom
     * @param x The superscripted number or whatever
     * @return min(Integer.MAX_VALUE, n ^ x)
     */
    public static int pow(int n, int x) {
        if (Math.log(Integer.MAX_VALUE) / Math.log(x) < n) {
            return Integer.MAX_VALUE;
        } else {
            int result = 1;
            for (int i = 0; i < x; i++) {
                result *= n;
            }
            return result;
        }
    }
}
