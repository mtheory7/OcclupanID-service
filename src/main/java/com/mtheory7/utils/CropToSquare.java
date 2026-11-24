package com.mtheory7.utils;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.datavec.image.data.ImageWritable;
import org.datavec.image.transform.BaseImageTransform;

import java.util.Random;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public class CropToSquare extends BaseImageTransform<Mat> {

    public CropToSquare() {
        super(null);
    }

    @Override
    protected ImageWritable doTransform(ImageWritable image, Random random) {
        if (image == null) {
            return null;
        }

        // Local converter per call for thread safety across concurrent ECS requests
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Mat mat = converter.convert(image.getFrame());
        int h = mat.rows();
        int w = mat.cols();

        if (h == w) {
            return image;
        }

        int side = Math.min(h, w);
        int x = (w - side) / 2;
        int y = (h - side) / 2;

        try (Rect roi = new Rect(x, y, side, side);
            Mat subMat = new Mat(mat, roi)) {
            // clone() ensures a contiguous pixel buffer before wrapping back into a Frame
            Mat cropped = subMat.clone();
            //imwrite("C:\\Users\\thesp\\Code\\OcclupanID-service\\transformedImages\\transformedImage.png", cropped);
            return new ImageWritable(converter.convert(cropped));
        }
    }
}