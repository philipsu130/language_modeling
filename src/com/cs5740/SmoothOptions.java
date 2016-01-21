package com.cs5740;

/**
 * Created by jink2 on 9/23/2015.
 */
public class SmoothOptions {
    int cutoff;

    private SmoothOptions(int cutoff) {
        this.cutoff = cutoff;
    }

    public static final SmoothOptions UNSMOOTHED = SMOOTH_WITH_CUTOFF(0);
    public static final SmoothOptions SMOOTHED = SMOOTH_WITH_CUTOFF(Integer.MAX_VALUE);
    public static final SmoothOptions DEFAULT = SMOOTH_WITH_CUTOFF(5);
    public static SmoothOptions SMOOTH_WITH_CUTOFF(int cutoff) {
        return new SmoothOptions(cutoff);
    }

    public int getCutoff() {
        return cutoff;
    }
}
