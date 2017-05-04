package org.jtrim2.swing.component;

/**
 * Defines the possible outcomes of rendering method calls.
 *
 * @see AsyncRenderingComponent
 * @see ImageRenderer
 * @see RenderingResult
 *
 * @author Kelemen Attila
 */
public enum RenderingType {
    /**
     * Means that no meaningful rendering was done by the renderer. The image
     * to which it was drawing to should be discarded. Note that the render
     * might have drawn something to the image but this should not be displayed
     * to the user.
     */
    NO_RENDERING,

    /**
     * Means that the renderer did some rendering which should be displayed to
     * the user but it is not significant.
     * That is, it means that the rendered image does not contain what is the
     * main interest of the user.
     * <P>
     * For example: When displaying an image, this might mean that some meta
     * information about the image to be rendered was drawn but the pixels of
     * the image were not.
     */
    INSIGNIFICANT_RENDERING,

    /**
     * Means that the renderer did some rendering which should be displayed to
     * the user and the rendering is significant to the user.
     * That is, it means that the render image contains what is the main
     * interest of the user.
     * <P>
     * For example: When displaying an image, this might mean that the actual
     * pixels of the image were drawn. Though, subsequent rendering might
     * further enhance the image.
     */
    SIGNIFICANT_RENDERING
}
