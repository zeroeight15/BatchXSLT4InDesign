package com.epaperarchives.batchxslt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.*;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
// Imported TraX classes
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * The JpgImage class can be used to resize JPEG images and
 * either return them as either BufferedImages or save them
 * as files. Here's a short example:
 * <p><blockquote><pre>
 * JpgImage ji = new JpgImage("picture.jpg");
 * ji.scalePercent(0.5);
 * ji.cropProportions(5, 7, true);
 * ji.sendToFile("new_picture.jpg");
 * </pre></blockquote><p>
 * Some of the code used in this class was a modification of
 * the excellent example code provided by Will Bracken in
 * the Java Developer's Forum, at:<br>
 * http://forum.java.sun.com/thread.jsp?thread=260711&forum=20&message=985157
 * <p>
 * The "Java Examples in a Nutshell" book by David Flanagan
 * was also a good reference. There are some nice examples of
 * using transform matrices to blur and sharpen an image in
 * there, if you want to add that functionality. While most
 * of the methods here are fairly trivial wrappers around various
 * transform objects, the rotate method is a bit more complicated,
 * due to the fact that the image dimensions and origin are
 * changed -- see the code itself for more details.
 * <p>
 * The Java 2D classes are used for the image manipulation,
 * so this will only work with Java 1.2 or higher. Also, the
 * com.sun.image.codec.jpeg.JPEGImageDecoder, 
 * com.sun.image.codec.jpeg.JPEGEncodeParam, and
 * com.sun.image.codec.jpeg.JPEGCodec classes are used to read and
 * save images, so this may not work with non-Sun implementations 
 * of Java. You may be able to use the more generic Image and/or 
 * ImageIcon classes to perform similar functions.
 * <p>
 * Program version 1.0. Author Julian Robichaux, http://www.nsftools.com
 *
 * @author Julian Robichaux ( http://www.nsftools.com )
 * @version 1.0
 */
public class JpgImage
{
  private static final double INCH_2_CM = 2.54;
	private BufferedImage bi = null;
	private String currentImagePathName = "";
	
	/**
	 * Creates a JpgImage from a specified file name
	 *
	 * @param  fileName    the name of a JPEG file
	 * @exception  IOException    if the file cannot be opened or read
	 */
	public JpgImage ()
	{ }
	public JpgImage (String fileName) throws IOException
	{
		FileInputStream fis = new FileInputStream(fileName);
                bi = ImageIO.read(fis);
		fis.close();
	}
	public void JpgImageDispose ()
	{
		if (bi != null) {
			bi.flush();
			bi = null;
		}
	}
	
	/**
	 * Creates a JpgImage from the specified BufferedImage
	 *
	 * @param  image    a BufferedImage object
	 * @exception  IOException    if the BufferedImage is null
	 */
	public JpgImage (BufferedImage image) throws IOException
	{
		if (image == null)
			throw new IOException("BufferedImage is null");
		else
			bi = image;
	}
	
	/**
	 * Returns the height (in pixels) of the current JpgImage object
	 * 
	 * @return  the height of the current image
	 */
	public int getHeight ()
	{
		return bi.getHeight();
	}
	
	/**
	 * Returns the width (in pixels) of the current JpgImage object
	 * 
	 * @return  the width of the current image
	 */
	public int getWidth ()
	{
		return bi.getWidth();
	}
	
	/**
	 * Shrinks or enlarges the current JpgImage object so that the
	 * height of the image (in pixels) equals the given height
	 *
	 * @param  height    scale the image to this height
	 */
	public void scaleHeight (int height)
	{
		double scale = (double)height / (double)bi.getHeight();
		scalePercent(scale);
	}
	
	/**
	 * Shrinks or enlarges the current JpgImage object so that the
	 * width of the image (in pixels) equals the given width
	 *
	 * @param  width    scale the image to this width
	 */
	public void scaleWidth (int width)
	{
		double scale = (double)width / (double)bi.getWidth();
		scalePercent(scale);
	}
	
	/**
	 * Shrinks or enlarges the current JpgImage object so that the
	 * size of the image (in pixels) is the greater of the height and width
	 * dictated by the parameters.<p>
	 * For example, if the image has to be enlarged by a factor of 60% 
	 * in order to be the given height, and it has to be enlarged by a
	 * factor of 80% in order to be the given width, then the image will
	 * be enlarged by 80% (the greater of the two). Use this method if
	 * you need to make sure that an image is <i>at least</i> the given height
	 * and width.
	 *
	 * @param  height    scale the image to at least this height
	 * @param  width    scale the image to at least this width
	 */
	public void scaleHeightWidthMax (int height, int width)
	{
		double scaleH = (double)height / (double)bi.getHeight();
		double scaleW = (double)width / (double)bi.getWidth();
		scalePercent(Math.max(scaleH, scaleW));
	}
	
	/**
	 * Shrinks or enlarges the current JpgImage object so that the
	 * size of the image (in pixels) is the lesser of the height and width
	 * dictated by the parameters.<p>
	 * For example, if the image has to be enlarged by a factor of 60% 
	 * in order to be the given height, and it has to be enlarged by a
	 * factor of 80% in order to be the given width, then the image will
	 * be enlarged by 60% (the lesser of the two). Use this method if
	 * you need to make sure that an image is <i>no larger</i> than the given
	 * height or width.
	 *
	 * @param  height    scale the image to at most this height
	 * @param  width    scale the image to at most this width
	 */
	public void scaleHeightWidthMin (int height, int width)
	{
		double scaleH = (double)height / (double)bi.getHeight();
		double scaleW = (double)width / (double)bi.getWidth();
		scalePercent(Math.min(scaleH, scaleW));
	}
	
	/**
	 * Shrinks or enlarges the current JpgImage object by the given scale
	 * factor, with a scale of 1 being 100% (or no change).<p>
	 * For example, if you need to reduce the image to 75% of the current size, 
	 * you should use a scale of 0.75. If you want to double the size of the
	 * image, you should use a scale of 2. If you attempt to scale using a
	 * negative number, the image will not be modified.
	 *
	 * @param  scale    the amount that this image should be scaled (1 = no change)
	 */
	public void scalePercent (double scale)
	{
		scalePercent (scale, 1, 1, null);
	}
	public void scalePercent (double scale, int antialias, int numSteps, BufferedImage theimage)
	{
		if ((scale < 0) || (scale == 1)) return;

		BufferedImage myimage = theimage;
		if (myimage == null) myimage = bi;

		RenderingHints hints = null;
		BufferedImage destImg = null;
		BufferedImage intermedImg = null;
		AffineTransformOp op = null;
		double myScale = scale;
		int mynumSteps = numSteps;
		if (mynumSteps <= 0) mynumSteps = 1;
		//if (mynumSteps > 4) mynumSteps = 4;
		if (antialias > 0) {
	    // antialias = 67 seems to result in best images
			hints = new RenderingHints(null);
			if ((antialias & 1) > 0)   hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if ((antialias & 2) > 0)   hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			if ((antialias & 4) > 0)   hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			if ((antialias & 8) > 0)   hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			if ((antialias & 16) > 0)  hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			if ((antialias & 32) > 0)  hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			if ((antialias & 64) > 0)  hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			if ((antialias & 128) > 0) hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		}
		double currentSize = 1.0;
		double endSize = scale;
		double newSize = 1.0;
		int remainSteps = mynumSteps;
		if (remainSteps <= 0) remainSteps = 1;
		double stepSize = (1.0 - endSize) / mynumSteps;

		// check, smoothing enhancement desired
		do {
			// calc new image size factor step by step
			newSize = (endSize + ((remainSteps-1)*stepSize)) / (endSize + (remainSteps*stepSize));
/*  this stuff using AffineTransformOp crashes !!!!
		System.out.println("****scalePercent 2  scale:" + scale + ", newSize:" + newSize);
			//System.out.println("Image newSize: " + newSize);
			// recalc image
			op = new AffineTransformOp(AffineTransform.getScaleInstance(newSize, newSize), hints);
		System.out.println("****scalePercent 3  scale:" + scale + ", antialias:" + antialias + ", numSteps:" + numSteps);
			intermedImg = op.createCompatibleDestImage(myimage, myimage.getColorModel());
		System.out.println("****scalePercent 4  intermedImg:" + intermedImg.getColorModel().toString());
			op.filter(myimage, intermedImg);
		System.out.println("****scalePercent 5  scale:" + scale + ", antialias:" + antialias + ", numSteps:" + numSteps);
*/
     // scale it
     intermedImg = scale(myimage, newSize, hints);

			myimage = intermedImg;
			// check if more steps needed
			if (--remainSteps <= 0) break;
			currentSize = newSize;
		} while(true);
		if (theimage == null) bi = myimage;
		else theimage = myimage;

		//System.out.println("Image scaled to: " + myimage.getWidth());
	}

private static BufferedImage scale(BufferedImage imageToScale, Double ratio, RenderingHints hints) {
    Integer dWidth = (int)(imageToScale.getWidth() * ratio);
    Integer dHeight = (int)(imageToScale.getHeight() * ratio);
    BufferedImage scaledImage = new BufferedImage(dWidth, dHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = scaledImage.createGraphics();
    //graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics2D.setRenderingHints(hints);
    graphics2D.drawImage(imageToScale, 0, 0, dWidth, dHeight, null);
    graphics2D.dispose();
    return scaledImage;
}
	/**
	 * Crops the current JpgImage object using the given proportions.
	 * The resulting image will be as large as possible, with either
	 * the width or the height of the image unchanged, and the other
	 * measurement of the image cropped equally on either side.
	 * <p>
	 * For example, to crop an image with the proportions of a 5x7 picture,
	 * you could pass a width of 5 and a height of 7 (or a width of 7 
	 * and a height of 5).
	 *
	 * @param  width    the proportional width to crop
	 * @param  height    the proportional height to crop
	 */
	public void cropProportions (double width, double height)
	{
		int currentHeight = bi.getHeight();
		int currentWidth = bi.getWidth();
		int cropHeight = (int)(currentWidth * (height / width));
		int cropWidth = (int)(currentHeight * (width / height));
		
		if (cropHeight > currentHeight) {
			bi = bi.getSubimage((int)((currentWidth - cropWidth) / 2), 0, 
					cropWidth, currentHeight);
		} else {
			bi = bi.getSubimage(0, (int)((currentHeight - cropHeight) / 2), 
					currentWidth, cropHeight);
		}
	}
	
	/**
	 * Crops the current JpgImage object using the given proportions,
	 * optionally "optimizing" the cropping by swapping the height and
	 * width proportions if doing so would crop less of the image.
	 * The resulting image will be as large as possible, with either
	 * the width or the height of the image unchanged, and the other
	 * measurement of the image cropped equally on either side.
	 * <p>
	 * For example, to crop an image with the proportions of a 5x7 picture,
	 * you could pass a width of 5 and a height of 7. In this case, if the
	 * "optimize" flag was set and the image was wider than it was tall,
	 * this method would automatically crop with proportions of 7x5 instead.
	 *
	 * @param  width    the proportional width to crop
	 * @param  height    the proportional height to crop
	 * @param  optimize    if true, indicates that the width and height can be
	 *                     swapped if that would cause less of the image to be
	 *                     cropped
	 */
	public void cropProportions (double width, double height, boolean optimize)
	{
		double big = Math.max(width, height);
		double small = Math.min(width, height);
		if (optimize) {
			if (bi.getWidth() > bi.getHeight())
				cropProportions(big, small);
			else
				cropProportions(small, big);
		} else {
			cropProportions(width, height);
		}
	}
	
	/**
	 * Crops the current JpgImage object using the given proportions,
	 *
	 * @param  infilePathName    the path to the image to cut
	 * @param  x    the left x position
	 * @param  y    the top y position
	 * @param  w    the width to crop
	 * @param  h    the height to crop
	 * @param  density the output density
	 * @param  quality the output quality
	 * @param  scale the scale factor
	 * @param  dpiscale the factor of actual dpi of input image / 72
	 * @param  outfilePathName    the path to the cut output image
	 * @param  overwriteexisting	overwrite an already existing output file
	 */
	public int cutSubJPGImage ( String infilePathName, int x, int y, int w, int h, int density, float quality, double scale, double dpiscale, String outfilePathName, int overwriteexisting)
	{
		/*
		System.out.println("cutSubJPGImage infilePathName: "+ infilePathName);
		System.out.println("cutSubJPGImage x: "+ x);
		System.out.println("cutSubJPGImage y: "+ y);
		System.out.println("cutSubJPGImage w: "+ w);
		System.out.println("cutSubJPGImage h: "+ h);
		System.out.println("cutSubJPGImage density: "+ density);
		System.out.println("cutSubJPGImage quality: "+ quality);
		System.out.println("cutSubJPGImage scale: "+ scale);
		System.out.println("cutSubJPGImage dpiscale: "+ dpiscale);
		System.out.println("cutSubJPGImage outfilePathName: "+ outfilePathName);
		System.out.println("cutSubJPGImage overwriteexisting: "+ overwriteexisting);
		*/
		File outfile = new File(outfilePathName);
		if (outfile.exists()) {
			if (overwriteexisting == 0) {	// do not overwrite if exists
				return(0);
			}
			try { outfile.delete(); } catch (Exception e) {}
		}

		if ((bi == null) || (currentImagePathName.equals(infilePathName) == false)) {
			try {
				bi = ImageIO.read(new File(infilePathName));
				currentImagePathName = infilePathName;
				//System.out.println("****cutSubJPGImage loaded: "+ infilePathName);
			} catch (Exception excp) {
				bi = null;
				currentImagePathName = "";
				return(-1);
			}
			if (bi == null) {
				currentImagePathName = "";
				return(-2);
			}
		}
		// make sure we do not try to cut outside the original image
		/*
		System.out.println("cutSubJPGImage infileWidth: "+ bi.getWidth());
		System.out.println("cutSubJPGImage infileheight: "+ bi.getHeight());
		*/
		int maxwidth = bi.getWidth();
		int maxheight = bi.getHeight();
		int cutX = x;
		int cutY = y;
		if (cutX < 0) cutX = 0;
		if (cutY < 0) cutY = 0;
		int cutwidth = w;
		int cutheight = h;
		if (cutX+cutwidth > maxwidth) cutwidth = maxwidth - cutX;
		if (cutY+cutheight > maxheight) cutheight = maxheight - cutY;

		BufferedImage cutimage;
		try {
			cutimage = bi.getSubimage(cutX, cutY, cutwidth, cutheight);
		} catch (Exception excpcut) {
			return(-3);
		}
		BufferedImage resizedimage = null;
		if ((scale != 1.0) || (dpiscale != 1.0)) {
			int newWidth = Double.valueOf(cutimage.getWidth() / dpiscale * scale).intValue();
			int newHeight = Double.valueOf(cutimage.getHeight() / dpiscale * scale).intValue();
			/*
			System.out.println("-- cutSubJPGImage newWidth: "+ newWidth);
			System.out.println("-- cutSubJPGImage newHeight: "+ newHeight);
			*/
			resizedimage = new BufferedImage(newWidth, newHeight, cutimage.getType());
			Graphics2D g = resizedimage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(cutimage, 0, 0, newWidth, newHeight, 0, 0, cutimage.getWidth(), cutimage.getHeight(), null);
			g.dispose();
			/*
			System.out.println("-- cutSubJPGImage resizedWidth: "+ resizedimage.getWidth());
			System.out.println("-- cutSubJPGImage resizedHeight: "+ resizedimage.getHeight());
			*/
		}
		
		FileImageOutputStream outs;
		try {
			outs = new FileImageOutputStream(new File(outfilePathName));
		} catch (IOException fnfex) {
			return(-4);
		}

//		JPEGEncodeParam param;
                // META DATA
                ImageReader imageReader;
                imageReader = ImageIO.getImageReadersBySuffix("jpeg").next();
                IIOMetadata metadata;
                try {
                    metadata = imageReader.getImageMetadata(0);
		} catch (IOException iorex) {
			return(-5);
		}
                Element tree;
                tree = (Element)metadata.getAsTree("javax_imageio_jpeg_image_1.0");
                Element jfif;
                jfif = (Element)tree.getElementsByTagName("app0JFIF").item(0);
                String dpiX = jfif.getAttribute("Xdensity");
                String dpiY = jfif.getAttribute("Ydensity");
                // IMAGE WRITER
                ImageWriter imageWriter = ImageIO.getImageWritersBySuffix("jpeg").next();
                imageWriter.setOutput(outfile);
                ImageWriteParam writeParams;
                writeParams = imageWriter.getDefaultWriteParam();
		writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		writeParams.setCompressionQuality(quality);
                metadata = imageWriter.getDefaultImageMetadata(new ImageTypeSpecifier(bi),writeParams);
                tree = (Element)metadata.getAsTree("javax_imageio_jpeg_image_1.0");
                jfif = (Element)tree.getElementsByTagName("app0JFIF").item(0);

                jfif.setAttribute("Xdensity",density+"");
                jfif.setAttribute("Ydensity",density+"");
                jfif.setAttribute("resUnits","1");    // dots per inch
		try {
			if (resizedimage != null) {
				imageWriter.write(metadata,new IIOImage(resizedimage,null,null), writeParams);
				resizedimage.flush();
				cutimage.flush();
			}
			else {
				imageWriter.write(metadata,new IIOImage(cutimage,null,null), writeParams);
				cutimage.flush();
			}
		} catch (IOException ex) {
			try { outs.close(); } catch (IOException exouts) { return(-6); }
			return(-7);
                }
		try { outs.close(); } catch (IOException ex) { return(-8); }
		
		return(0);
	}
	
	/**
	 * Rotates the current JpgImage object a specified number of degrees,
	 * with a default background color of white (also see the notes for 
	 * the rotate(double, Color) method). This is the equivalent of calling:
	 * <p>
	 * <pre>JpgImage.rotate(degrees, Color.white);</pre>
	 *
	 * @param  degrees    the number of degrees to rotate the image
	 */
	public void rotate (double degrees)
	{
		rotate(degrees, Color.white);
	}
	
	/**
	 * Rotates the current JpgImage object a specified number of degrees.
	 * <p>
	 * You should be aware of 2 things with regard to image rotation.
	 * First, the more times you rotate an image, the more the image 
	 * degrades. So instead of rotating an image 90 degrees and then
	 * rotating it again 45 degrees, you should rotate it once at a
	 * 135 degree angle. 
	 * <p>
	 * Second, a rotated image will always have a rectangular border 
	 * with sides that are vertical and horizontal, and all of the area
	 * within this border will become part of the resulting image. 
	 * Therefore, if you rotate an image at an angle that's not a 
	 * multiple of 90 degrees, your image will appear to be placed 
	 * at an angle against a rectangular background of the specified Color. 
	 * For this reason, an image rotated 45 degrees and then another 45 degrees
	 * will not be the same as an image rotated 90 degrees.
	 *
	 * @param  degrees    the number of degrees to rotate the image
	 * @param  backgroundColor    the background color used for areas
	 *                            in the resulting image that are not
	 *                            covered by the image itself
	 */
	public void rotate (double degrees, Color backgroundColor)
	{
		/*
		 * Okay, this required some strange geometry. Before an image
		 * is rotated, the origin is at the top left corner of the 
		 * rectangle that contains the image. After an image is rotated,
		 * you want the origin to get moved to a spot that will allow
		 * the entire rotated image to be framed within a rectangle.
		 * Unfortunately, this does not happen automatically.
		 *
		 * That's where the strange geometry comes in. We essentially
		 * need to rotate the image, and then determine what the width
		 * and height of the new image is, and then determine where the
		 * new origin should be. The width and height is easy (you can
		 * also use the AffineTransform getWidth and getHeight methods),
		 * but the new origin...well...not so easy. Unfortunately, my
		 * trigonometry skills aren't sharp enough to be able to give you
		 * a good explanation of what's going on with this method without
		 * drawing everything out for you. If you want to figure it out
		 * for yourself, just draw an axis on a sheet of paper, place a
		 * smaller rectangular piece of paper on the axis, and start
		 * rotating it along the axis to see what's going on. Then pull
		 * out your old trig books and start calculating.
		 *
		 * BTW, if there's an easier way to do this, I'd love to know about it.
		 */
		
		// adjust the angle that was passed so it's between 0 and 360 degrees
		double positiveDegrees = (degrees % 360) + ((degrees < 0) ? 360 : 0);
		double degreesMod90 = positiveDegrees % 90;
		double radians = Math.toRadians(positiveDegrees);
		double radiansMod90 = Math.toRadians(degreesMod90);
		
		// don't bother with any of the rest of this if we're not really rotating
		if (positiveDegrees == 0)
			return;
		
		// figure out which quadrant we're in (we'll want to know this later)
		int quadrant = 0;
		if (positiveDegrees < 90)
			quadrant = 1;
		else if ((positiveDegrees >= 90) && (positiveDegrees < 180))
			quadrant = 2;
		else if ((positiveDegrees >= 180) && (positiveDegrees < 270))
			quadrant = 3;
		else if (positiveDegrees >= 270)
			quadrant = 4;
		
		// get the height and width of the rotated image (you can also do this
		// by applying a rotational AffineTransform to the image and calling
		// getWidth and getHeight against the transform, but this should be a
		// faster calculation)
		int height = bi.getHeight();
		int width = bi.getWidth();
		double side1 = (Math.sin(radiansMod90) * height) + (Math.cos(radiansMod90) * width);
		double side2 = (Math.cos(radiansMod90) * height) + (Math.sin(radiansMod90) * width);
		
		double h = 0;
		int newWidth = 0, newHeight = 0;
		if ((quadrant == 1) || (quadrant == 3)) {
			h = (Math.sin(radiansMod90) * height);
			newWidth = (int)side1;
			newHeight = (int)side2;
		} else {
			h = (Math.sin(radiansMod90) * width);
			newWidth = (int)side2;
			newHeight = (int)side1;
		}
		
		// figure out how much we need to shift the image around in order to
		// get the origin where we want it
		int shiftX = (int)(Math.cos(radians) * h) - ((quadrant == 3) || (quadrant == 4) ? width : 0);
		int shiftY = (int)(Math.sin(radians) * h) + ((quadrant == 2) || (quadrant == 3) ? height : 0);
		
		// create a new BufferedImage of the appropriate height and width and
		// rotate the old image into it, using the shift values that we calculated
		// earlier in order to make sure the new origin is correct
		BufferedImage newbi = new BufferedImage(newWidth, newHeight, bi.getType());
		Graphics2D g2d = newbi.createGraphics();
		g2d.setBackground(backgroundColor);
		g2d.clearRect(0, 0, newWidth, newHeight);
		g2d.rotate(radians);
		g2d.drawImage(bi, shiftX, -shiftY, null);
		bi = newbi;
	}
	
	/**
	 * Inverts the current JpgImage object
	 */
	public void invert ()
	{
		AffineTransform at = AffineTransform.getTranslateInstance(bi.getWidth(), 0);
		at.scale(-1.0, 1.0);
		AffineTransformOp op = new AffineTransformOp(at, null);
		bi = op.filter(bi, null);
	}
	
	/**
	 * Makes the current JpgImage object a greyscale image
	 */
	public void grayscale ()
	{
		ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
		bi = op.filter(bi, null);
	}
	
	/**
	 * Makes the current JpgImage object a negative of the original image
	 */
	public void negative ()
	{
		RescaleOp op = new RescaleOp(-1.0f, 255f, null);
		bi = op.filter(bi, null);
	}
	
	/**
	 * Returns the current JpgImage object as a BufferedImage
	 *
	 * @return  a BufferedImage representing the current JpgImage
	 */
	public BufferedImage sendToBufferedImage ()
	{
		return bi;
	}
	
	/**
	 * Writes the current JpgImage object to a file, with a quality
	 * of 0.75
	 *
	 * @param  fileName    the name of the file to write the image to
	 *                     (if the file already exists, it will be
	 *                     overwritten)
	 * @exception  IOException    if there is an error writing to the file
	 */
	public void sendToFile (String fileName) throws IOException
	{
		sendToFile(fileName, 0.75f);
	}
	
	/**
	 * Writes the current JpgImage object to a file, with the
	 * specified quality
	 *
	 * @param  fileName    the name of the file to write the image to
	 *                     (if the file already exists, it will be
	 *                     overwritten)
	 * @param  quality     the JPEG quality of the resulting image file,
	 *                     from 0 to 1
	 * @param  imgWidth    the JPEG image display width to write to meta data,
	 * @param  imgHeight   the JPEG image display height to write to meta data,
	 * @exception  IOException    if there is an error writing to the file
	 */
	public void sendToFile (String fileName, float quality) throws IOException {
		sendToFile (fileName, quality, 0, 0, 0);
	}
	public void sendToFile (String fileName, float quality, int dpi) throws IOException {
		sendToFile (fileName, quality, dpi, 0, 0);
	}
	public void sendToFile (String fileName, float quality, int dpi, int imgWidth, int imgHeight) throws IOException
	{
		if (quality < 0) quality = 0f;
		if (quality > 1) quality = 1f;
		
		File outf = new File(fileName);
    ImageOutputStream out = ImageIO.createImageOutputStream(outf);

    //ImageWriter imageWriter = ImageIO.getImageWritersBySuffix("jpeg").next();
    IIOMetadata metadata = null;
    ImageWriteParam writeParams = null;
    ImageWriter imageWriter = null;
    for (Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("jpg"); imageWriters.hasNext();) {
       imageWriter = imageWriters.next();
       writeParams = imageWriter.getDefaultWriteParam();
		   writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		   writeParams.setCompressionQuality(quality);
       metadata = imageWriter.getDefaultImageMetadata(new ImageTypeSpecifier(bi), writeParams);
       if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
		      System.out.println("\n## imageWriter NOT suitable");
          continue;
       }
       break;
    }
    if (imageWriter == null) {
		  System.out.println("###FATAL ERROR: no imageWriter found!");
		  return;
    }




    /* DEBUG: list all metadata elements BEFORE change */
    /*
		System.out.println("\n+++ nativeMetadataFormatName: " + metadata.getNativeMetadataFormatName());
		System.out.println("+++ metadata BEFORE change: ");
    listMetadataTree (metadata);
    */


		IIOMetadataNode metadata_tree = null;
		metadata_tree = (IIOMetadataNode)metadata.getAsTree("javax_imageio_jpeg_image_1.0");

    // write image width/height to 'Dimension' metadata
		//System.out.println("+++ imgWidth: " + Integer.toString(imgWidth) + ",  imgHeight: " + Integer.toString(imgHeight));
    if (/*false &&*/ ((imgWidth > 0) || (imgHeight > 0))) {
      // check: https://www.fileformat.info/convert/image/metadata.htm for metadata of an image as XML

      // write to JFIF marker
			Element sof;
			sof = (Element)metadata_tree.getElementsByTagName("sof").item(0);

      sof.setAttribute("numLines",Integer.toString(imgHeight));
      sof.setAttribute("samplesPerLine",Integer.toString(imgWidth));

      metadata.setFromTree("javax_imageio_jpeg_image_1.0", metadata_tree);
    }

    // DPI: write Xdensity and/or Ydensity to JFIF metadata
		if (dpi > 0) {	// set dpi

      // write to JFIF marker
			Element jfif;
			jfif = (Element)metadata_tree.getElementsByTagName("app0JFIF").item(0);

      // set X and Y density,
			// set density unit "resUnits"
			// 1 = dots per inch
			// 2 = dots per centimeter
      jfif.setAttribute("Xdensity",Integer.toString(dpi));
      jfif.setAttribute("Ydensity",Integer.toString(dpi));
      jfif.setAttribute("resUnits","1");    // dots per inch
      
      metadata.setFromTree("javax_imageio_jpeg_image_1.0", metadata_tree);
		}

    /*
		System.out.println("+++ metadata AFTER change: ");
    listMetadataTree (metadata);
    */

    imageWriter.setOutput(out);
    imageWriter.write(null,new IIOImage(bi,null,metadata), writeParams);

		out.close();
		imageWriter.dispose();
		
		/* DEBUG: list metadata from new file */
		/*
		System.out.println("\n***metadata from file: " + fileName + "\n");
		getMetadata( fileName, true );
		*/
	}
	
	/**
	 * get graphics context
         * @return Graphics2D object
	 */
	public Graphics2D getImageGraphics () {
		return((Graphics2D)bi.getGraphics());
	}

  /* setDPIdimenson is NOT used: alredy set correct */
  private void setDPIdimenson(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {

    // for PMG, it's dots per millimeter
    double dotsPerMilli = 1.0 * dpi / 10 / INCH_2_CM;

		IIOMetadataNode metadata_tree = null;
		metadata_tree = (IIOMetadataNode)metadata.getAsTree("javax_imageio_1.0");

	  Element HorizontalPixelSize;
	  NodeList HorizontalPixelSizeNodeList = metadata_tree.getElementsByTagName("HorizontalPixelSize");
	  if (HorizontalPixelSizeNodeList.getLength() < 1) {
	    HorizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
	  }
	  else HorizontalPixelSize = (Element)HorizontalPixelSizeNodeList.item(0);
    HorizontalPixelSize.setAttribute("value", Double.toString(dotsPerMilli));

	  Element VerticalPixelSize;
	  NodeList VerticalPixelSizeNodeList = metadata_tree.getElementsByTagName("VerticalPixelSize");
	  if (VerticalPixelSizeNodeList.getLength() < 1) {
	    VerticalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
	  }
	  else VerticalPixelSize = (Element)VerticalPixelSizeNodeList.item(0);
    VerticalPixelSize.setAttribute("value", Double.toString(dotsPerMilli));


    IIOMetadataNode dim = new IIOMetadataNode("Dimension");
    dim.appendChild(HorizontalPixelSize);
    dim.appendChild(VerticalPixelSize);

    IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
    root.appendChild(dim);


		System.out.println("+++ metadata_tree BEFORE PixelSize change: ");
    System.out.println(serializeNode (root));

    metadata.mergeTree("javax_imageio_1.0", root);
		System.out.println("+++ metadata AFTER PixelSize change: ");
    listMetadataTree (metadata);

/*
    IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
    horiz.setAttribute("value", Double.toString(dotsPerMilli));

    IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
    vert.setAttribute("value", Double.toString(dotsPerMilli));

    IIOMetadataNode dim = new IIOMetadataNode("Dimension");
    dim.appendChild(horiz);
    dim.appendChild(vert);

    IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
    root.appendChild(dim);
    System.out.println("\n***new Dimension: " + serializeNode (root));

    metadata.mergeTree("javax_imageio_1.0", root);
*/
  }
 
 
	public IIOMetadata getMetadata( String fileName, boolean do_print ) 
	{
	  IIOMetadata metadata = null;
	  ImageInputStream iis = null;
		try {

			File file = new File( fileName );
			iis = ImageIO.createImageInputStream(file);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

			if (readers.hasNext()) {

					// pick the first available ImageReader
					ImageReader reader = readers.next();
					// attach source to the reader
					reader.setInput(iis, true);
					// read metadata of first image
					metadata = reader.getImageMetadata(0);
					reader.dispose();
			}
		}
		catch (Exception e) {
				e.printStackTrace();
		}
		finally {
		  if (iis != null) {
		    try {
		      iis.close();
		    }
		    catch(IOException e) {}
		  }
		}
		if (do_print) {
		  listMetadataTree (metadata);
		  return null;
		}
		return (metadata);
	}


	public void listMetadataTree (IIOMetadata metadata)
	{

	  // list all metadata root elements
		String[] metaFormatNames = metadata.getMetadataFormatNames();
		System.out.println("\n** metadata root elements **"); 
		for (String s: metaFormatNames) {           
			//Do your stuff here
			System.out.println("--" + s);
			
		}

		// list all metadata child nodes
		for (int mfn = 0; mfn < metaFormatNames.length; mfn++) {
			Element metadata_tree = null;
			metadata_tree = (Element)metadata.getAsTree(metaFormatNames[mfn]);

			System.out.println("++ childNodes of '" + metaFormatNames[mfn] + "'");
			NodeList childNodes = metadata_tree.getChildNodes();
			for (int n = 0; n < childNodes.getLength(); n++) {           
				//Do your stuff here
				System.out.println("----Node " + childNodes.item(n).getNodeName());
				NodeList childNodes2 = childNodes.item(n).getChildNodes();
				for (int n2 = 0; n2 < childNodes2.getLength(); n2++) {           
					//System.out.println("    --" + childNodes2.item(n2).getNodeName());
					
					String serial = serializeNode (childNodes2.item(n2));
					System.out.println("      " + serial);
				}
			}
    }
	
  }


	public String serializeNode (Node node)
	{

		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
		  transformer = transFactory.newTransformer();
		}
    catch (TransformerConfigurationException e) {
				BatchXSLT.g_mainXSLTFrame.showMess( "#### serializeNode TransformerConfigurationException:" + e.getMessage() + "'\n" );
				e.printStackTrace();
				return "";
		}
		StringWriter buffer = new StringWriter();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		try {
		  transformer.transform(new DOMSource(node), new StreamResult(buffer));
		}
    catch (TransformerException e) {
				BatchXSLT.g_mainXSLTFrame.showMess( "#### serializeNode TransformerException:" + e.getMessage() + "'\n" );
				e.printStackTrace();
				return "";
		}
		String str = buffer.toString();
		return str;
  }


}
