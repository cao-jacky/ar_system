package fi.fivegear.remar;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.Highgui;

/**
 * Created by st0rm23 on 2017/2/19.
 */

public class Constants {
    static public final int MESSAGE_META = 0;
    static public final int IMAGE_DETECT_SEGMENTED = 1;
    static public final int IMAGE_DETECT_COMPLETE = 2;
    static public final int IMAGE_DETECT = 3;
    static public final int PACKET_STATUS = 4;
    static public final int RESULTS_STATUS = 5;

    static public final int previewWidth = 1920;
    static public final int previewHeight = 1080;
    static public final int scale = 4;
    static public final int recoScale = 7;
    static public final int dispScale = 9;
    static public final String TAG = "ReMAR";
    static public final double[][] cameraMatrixData = new double[][]{{3.9324438974006659e+002, 0, 240}, {0, 3.9324438974006659e+002, 135}, {0, 0, 1}};
    static private final double[] distCoeffsData = new double[]{0, 0, 0, 0, 0};
    static private final double[][] cvToGlData = new double[][]{{1.0, 0, 0, 0}, {0, -1.0, 0, 0}, {0, 0, -1.0, 0}, {0, 0, 0, 1.0}};

    static public final boolean Show2DView = true;

    static public final MatOfDouble distCoeffs = new MatOfDouble();
    static public final Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
    static public final Mat cvToGl = new Mat(4, 4, CvType.CV_64FC1);

    static public final int RES_SIZE = 516;
    static public final int ACK_SIZE = 16;

    static {
        for(int i = 0; i < 3; i++)
            for(int j = 0; j < 3; j++)
                cameraMatrix.put(i, j, Constants.cameraMatrixData[i][j]);
        distCoeffs.fromArray(Constants.distCoeffsData);
        for(int i = 0; i < 4; i++)
            for(int j = 0; j < 4; j++)
                cvToGl.put(i, j, Constants.cvToGlData[i][j]);
    }
}
