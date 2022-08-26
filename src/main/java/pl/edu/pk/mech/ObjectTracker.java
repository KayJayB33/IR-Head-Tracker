package pl.edu.pk.mech;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_imgproc.CvMoments;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class ObjectTracker {

    final int INTERVAL = 10;// 1sec

    private static final Logger LOGGER = Logger.getLogger(ObjectTracker.class.getName());

    /**
     * Correct the color range- it depends upon the object, camera quality,
     * environment.
     */
    static CvScalar rgba_min = cvScalar(100, 100, 100, 0);// RED wide dabur birko
    static CvScalar rgba_max = cvScalar(255, 255, 255, 0);

    IplImage image;
    int ii = 0;

    public Frame track(Frame frame) {
        try {
            final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            final IplImage img = converter.convert(frame);

            int posX = 0;
            int posY = 0;

            if (img != null) {
                // show image on window
                cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise
                final IplImage detectThrs = getThresholdImage(img);

                CvMoments moments = new CvMoments();
                cvMoments(detectThrs, moments, 1);
                double mom10 = cvGetSpatialMoment(moments, 1, 0);
                double mom01 = cvGetSpatialMoment(moments, 0, 1);
                double area = cvGetCentralMoment(moments, 0, 0);
                posX = (int) (mom10 / area);
                posY = (int) (mom01 / area);
                // only if its a valid position
                if (posX > 0 && posY > 0) {
                    paint(img, posX, posY);
                }

                return converter.convert(detectThrs);
            }
        } catch (Exception e) {
            LOGGER.info("Exception: " + e.getMessage());
        }

        return null;
    }

    private void paint(IplImage img, int posX, int posY) {
//        Graphics g = jp.getGraphics();
//        path.setSize(img.width(), img.height());
//        // g.clearRect(0, 0, img.width(), img.height());
//        g.setColor(Color.RED);
//        // g.fillOval(posX, posY, 20, 20);
//        g.drawOval(posX, posY, 20, 20);
        LOGGER.info(posX + " , " + posY);
    }

    private IplImage getThresholdImage(IplImage orgImg) {
        IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
        //
        cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold);// red

        cvSmooth(imgThreshold, imgThreshold, CV_MEDIAN, 15, 0, 0, 0);
        //cvSaveImage(++ii + "dsmthreshold.jpg", imgThreshold);
        return imgThreshold;
    }


    public IplImage Equalize(BufferedImage bufferedimg) {
        Java2DFrameConverter converter1 = new Java2DFrameConverter();
        OpenCVFrameConverter.ToIplImage converter2 = new OpenCVFrameConverter.ToIplImage();
        IplImage iploriginal = converter2.convert(converter1.convert(bufferedimg));
        IplImage srcimg = IplImage.create(iploriginal.width(), iploriginal.height(), IPL_DEPTH_8U, 1);
        IplImage destimg = IplImage.create(iploriginal.width(), iploriginal.height(), IPL_DEPTH_8U, 1);
        cvCvtColor(iploriginal, srcimg, CV_BGR2GRAY);
        cvEqualizeHist(srcimg, destimg);
        return destimg;
    }
}
