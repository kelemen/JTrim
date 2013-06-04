package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.image.ImageData;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class AffineTransformationStep implements ImageTransformationStep {
    private final AffineTransform transformations;
    private final Color bckgColor;
    private final int interpolationType;

    public AffineTransformationStep(
            BasicImageTransformations transformations,
            Color bckgColor,
            InterpolationType interpolationType) {
        this(AffineImageTransformer.getTransformationMatrix(transformations),
                bckgColor,
                interpolationType);
    }

    public AffineTransformationStep(
            AffineTransform transformations,
            Color bckgColor,
            InterpolationType interpolationType) {
        ExceptionHelper.checkNotNullArgument(transformations, "transformations");
        ExceptionHelper.checkNotNullArgument(bckgColor, "bckgColor");
        ExceptionHelper.checkNotNullArgument(interpolationType, "interpolationType");

        this.transformations = new AffineTransform(transformations);
        this.bckgColor = bckgColor;

        switch (interpolationType) {
            case BILINEAR:
                this.interpolationType = AffineTransformOp.TYPE_BILINEAR;
                break;
            case BICUBIC:
                this.interpolationType = AffineTransformOp.TYPE_BICUBIC;
                break;
            default:
                this.interpolationType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
                break;
        }
    }

    public static AffineTransform getTransformationMatrix(
            AffineTransform transformations,
            double srcWidth, double srcHeight,
            double destWidth, double destHeight) {

        double srcAnchorX = srcWidth / 2.0;
        double srcAnchorY = srcHeight / 2.0;

        double destAnchorX = destWidth / 2.0;
        double destAnchorY = destHeight / 2.0;

        AffineTransform affineTransf = new AffineTransform();
        affineTransf.translate(destAnchorX, destAnchorY);
        affineTransf.concatenate(transformations);
        affineTransf.translate(-srcAnchorX, -srcAnchorY);

        return affineTransf;
    }

    private static boolean isSourceVisible(BufferedImage src,
            BufferedImage dest, AffineTransform transf) {

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int destWidth = dest.getWidth();
        int destHeight = dest.getHeight();

        Point2D destPoint = new Point2D.Double();

        transf.transform(new Point2D.Double(0, 0), destPoint);

        Polygon resultArea = new Polygon();

        transf.transform(new Point2D.Double(0, 0), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        transf.transform(new Point2D.Double(0, srcHeight), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        transf.transform(new Point2D.Double(srcWidth, srcHeight), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        transf.transform(new Point2D.Double(srcWidth, 0), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        return resultArea.intersects(0, 0, destWidth, destHeight);
    }

    private void transformImageTo(
            BufferedImage srcImage,
            AffineTransform affineTransf,
            BufferedImage drawingSurface, Graphics2D g) {

        int destWidth = drawingSurface.getWidth();
        int destHeight = drawingSurface.getHeight();

        g.setBackground(bckgColor);
        g.clearRect(0, 0, destWidth, destHeight);

        if (affineTransf.getDeterminant() != 0.0) {
            // This check is required because if the offset is too large
            // the drawImage seems to enter into an infinite loop.
            // This is possibly because of a floating point overflow.
            if (isSourceVisible(srcImage, drawingSurface, affineTransf)) {
                g.drawImage(srcImage, new AffineTransformOp(affineTransf, interpolationType), 0, 0);
            }
        }
        else {
            // In case the determinant is zero, the transformation
            // transforms the image to a line or a point which means
            // that the result is not visible.
            // Note however that the image was already cleared.
            // g.clearRect(0, 0, destWidth, destHeight);
        }
    }

    @Override
    public TransformedImage render(
            CancellationToken cancelToken,
            TransformationStepInput input,
            BufferedImage offeredBuffer) {

        BufferedImage inputImage = input.getInputImage().getImage();
        if (inputImage == null) {
            return TransformedImage.NULL_IMAGE;
        }

        int destWidth = input.getDestinationWidth();
        int destHeight = input.getDestinationHeight();
        if (destWidth <= 0 || destHeight <= 0) {
            return TransformedImage.NULL_IMAGE;
        }

        BufferedImage drawingSurface;
        if (offeredBuffer != null
                && offeredBuffer.getType() == inputImage.getType()
                && offeredBuffer.getWidth() == destWidth
                && offeredBuffer.getHeight() == destHeight) {
            drawingSurface = offeredBuffer;
        }
        else {
            drawingSurface = ImageData.createCompatibleBuffer(inputImage, destWidth, destHeight);
        }

        AffineTransform affineTransf = getTransformationMatrix(
                transformations,
                inputImage.getWidth(),
                inputImage.getHeight(),
                destWidth,
                destHeight);
        Graphics2D g = drawingSurface.createGraphics();
        try {
            transformImageTo(inputImage, affineTransf, drawingSurface, g);
        } finally {
            g.dispose();
        }

        return new TransformedImage(drawingSurface, new AffineImagePointTransformer(affineTransf));
    }

    public AffineTransform getTransformations() {
        return new AffineTransform(transformations);
    }

    public Color getBackgroundColor() {
        return bckgColor;
    }

    public int getInterpolationType() {
        return interpolationType;
    }

    /**
     * Returns the string representation of this transformation in no
     * particular format
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "Affine transformation: " + transformations
                + "\nuse interpolation " + interpolationType;
    }
}
