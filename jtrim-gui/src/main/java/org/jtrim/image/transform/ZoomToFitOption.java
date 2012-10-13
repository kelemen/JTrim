package org.jtrim.image.transform;

/**
 * Defines the possible rules which should be applied when scaling an image to
 * fit a particular display.
 * <P>
 * Note that usually you need to specify at least {@link #FIT_HEIGHT} or
 * {@link #FIT_WIDTH}, otherwise it is ill-defined what an implementation should
 * do.
 * <P>
 * <B>Warning</B>: New rules can be added at any time and rules can be ignored
 * by the using code if it does not support or recognize a particular rule.
 */
public enum ZoomToFitOption {
    /**
     * Means that the width/height ratio of the image must not be changed when
     * scaling. If this rule is not applied, implementations may stretch the
     * image in both dimensions to fit the display.
     */
    KEEP_ASPECT_RATIO,

    /**
     * Means that the image need to be magnified if necessary to fit the
     * display. If this rule is not applied, the image will only be shrinked but
     * will never be magnified.
     */
    MAY_MAGNIFY,

    /**
     * Means that the image need to be scaled so that no part of the image
     * should be beyond the left or right side of the display.
     */
    FIT_WIDTH,

    /**
     * Means that the image need to be scaled so that no part of the image
     * should be beyond the top or bottom side of the display.
     */
    FIT_HEIGHT
}
