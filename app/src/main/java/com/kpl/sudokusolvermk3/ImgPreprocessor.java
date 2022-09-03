package com.kpl.sudokusolvermk3;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ImgPreprocessor {

    private static final int BZ_THRESHOLD = 127;

    public Bitmap givenImg;

    public ImgPreprocessor(Bitmap givenImg) {

        this.givenImg = givenImg;
    }

    public void binarize() {

        int width = givenImg.getWidth();
        int height = givenImg.getHeight();

        for (int i = 0; i < width; i++) {

            for (int j = 0; j < height; j++) {

                int pixel = givenImg.getPixel(i, j);

                if (pixel < BZ_THRESHOLD) {

                    givenImg.setPixel(i, j, Color.BLACK);
                } else {

                    givenImg.setPixel(i, j, Color.WHITE);
                }
            }
        }
    }
}
