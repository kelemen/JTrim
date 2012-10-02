/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import org.jtrim.image.ImageData;

/**
 *
 * @author Kelemen Attila
 */
public final class AffineImageTransformer implements ImageTransformer {
    private static final double RAD_0;
    private static final double RAD_90;
    private static final double RAD_180;
    private static final double RAD_270;

    static {
        BasicImageTransformations.Builder radConvTest;
        radConvTest = new BasicImageTransformations.Builder();

        radConvTest.setRotateInDegrees(0);
        RAD_0 = radConvTest.getRotateInRadians();

        radConvTest.setRotateInDegrees(90);
        RAD_90 = radConvTest.getRotateInRadians();

        radConvTest.setRotateInDegrees(180);
        RAD_180 = radConvTest.getRotateInRadians();

        radConvTest.setRotateInDegrees(270);
        RAD_270 = radConvTest.getRotateInRadians();
    }

    public static AffineTransform getTransformationMatrix(
            BasicImageTransformations transformations,
            double srcWidth, double srcHeight,
            double destWidth, double destHeight) {

        double srcAnchorX = (srcWidth - 1.0) * 0.5;
        double srcAnchorY = (srcHeight - 1.0) * 0.5;

        double destAnchorX = (destWidth - 1.0) * 0.5;
        double destAnchorY = (destHeight - 1.0) * 0.5;

        AffineTransform affineTransf = new AffineTransform();
        affineTransf.translate(transformations.getOffsetX(), transformations.getOffsetY());
        affineTransf.translate(destAnchorX, destAnchorY);
        affineTransf.rotate(transformations.getRotateInRadians());
        if (transformations.isFlipHorizontal()) affineTransf.scale(-1.0, 1.0);
        if (transformations.isFlipVertical()) affineTransf.scale(1.0, -1.0);
        affineTransf.scale(transformations.getZoomX(), transformations.getZoomY());
        affineTransf.translate(-srcAnchorX, -srcAnchorY);

        return affineTransf;
    }

    public static AffineTransform getTransformationMatrix(
            BasicImageTransformations transformations,
            ImageTransformerData input) {

        BufferedImage srcImage = input.getSourceImage();
        if (srcImage == null) {
            return null;
        }

        return getTransformationMatrix(transformations,
                srcImage.getWidth(), srcImage.getHeight(),
                input.getDestWidth(), input.getDestHeight());
    }

    public static boolean isSimpleTransformation(
            BasicImageTransformations transformation) {

        double radRotate = transformation.getRotateInRadians();

        return (transformation.getZoomX() == 1.0 &&
                transformation.getZoomY() == 1.0 &&
                (radRotate == RAD_0 ||
                radRotate == RAD_90 ||
                radRotate == RAD_180 ||
                radRotate == RAD_270));
    }

    private final BasicImageTransformations transformations;
    private final Color bckgColor;
    private final int interpolationType;

    public AffineImageTransformer(BasicImageTransformations transformations,
            Color bckgColor, InterpolationType interpolationType) {

        this.transformations = transformations;
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
                try {
                    g.drawImage(srcImage, new AffineTransformOp(affineTransf, interpolationType), 0, 0);
                } catch (ImagingOpException ex) {
                    throw new ImageProcessingException(ex);
                }
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
    public TransformedImage convertData(ImageTransformerData input) {
        BufferedImage srcImage = input.getSourceImage();
        if (srcImage == null) {
            return new TransformedImage(null, null);
        }

        AffineTransform affineTransf = getTransformationMatrix(transformations, input);

        BufferedImage drawingSurface;

        drawingSurface = ImageData.createCompatibleBuffer(
                srcImage, input.getDestWidth(), input.getDestHeight());

        Graphics2D g = drawingSurface.createGraphics();
        try {
            transformImageTo(srcImage, affineTransf, drawingSurface, g);
        } finally {
            g.dispose();
        }


        return new TransformedImage(drawingSurface, new AffineImagePointTransformer(affineTransf));
    }

    @Override
    public String toString() {
        return "Affine transformation: " + transformations
                + "\nuse interpolation " + interpolationType;
    }

}
