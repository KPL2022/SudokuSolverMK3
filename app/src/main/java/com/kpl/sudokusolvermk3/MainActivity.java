package com.kpl.sudokusolvermk3;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    protected ImageView mainWindow;
    protected Bitmap src;
    protected Bitmap base;
    protected Button testButton;
    protected ImageButton solv;
    protected double epi = 0.15;

    /**
     * 1. about tmp file creation for 2nd uri, and options to use free style cropping
     * TODO: other options worth, check them out?
     * https://www.programcreek.com/java-api-examples/?api=com.yalantis.ucrop.UCrop
     *
     * 2. kotlin example https://www.youtube.com/watch?v=71gh86AZ8mI
     *
     * 3. documentation
     * https://github.com/Yalantis/uCrop
     * */

    private ActivityResultContract<List<Uri>, Uri> cropContract = new ActivityResultContract<List<Uri>, Uri>() {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, List<Uri> input) {

            Uri src = input.get(0);
            Uri dest = input.get(1);

            UCrop.Options opts = new UCrop.Options();

            opts.setFreeStyleCropEnabled(true);

            return UCrop.of(src, dest).withOptions(opts).getIntent(context);
        }

        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            return UCrop.getOutput(intent);
        }
    };

    ActivityResultLauncher<List<Uri>> mGetCropped = registerForActivityResult(cropContract, new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {

            src = null;

            try {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {

                    src = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getApplicationContext().getContentResolver(), result));
                } else {

                    src = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), result);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            src = src.copy(Bitmap.Config.RGB_565, true);
            src = scaleToSize(src, 400, true);

            base = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(base);

            ColorMatrix cm = new ColorMatrix(new float[] {
                    255, 0, 0, 0, -128*255,
                    0, 255, 0, 0, -128*255,
                    0, 0, 255, 0, -128*255,
                    0, 0, 0, 1, 0
            });

            ColorMatrixColorFilter cmcf = new ColorMatrixColorFilter(cm);
            Paint pt = new Paint();
            pt.setColorFilter(cmcf);

            canvas.drawBitmap(src, 0, 0, pt);

            // TODO: this is problematic, on rotation, the padded area is auto black and gets counted into scores
            // TODO: also, if rotation is applied in the end, need to adjust board dimension estimations later
            base = deskew(base);

            /**
             * https://towardsdatascience.com/pre-processing-in-ocr-fc231c6035a7
             *
             * https://stackoverflow.com/questions/29982528/how-do-i-rotate-a-bitmap-in-android
             *
             * https://stackoverflow.com/questions/36474343/convert-all-colors-other-than-a-particular-color-in-a-bitmap-to-white
             *
             * hardware vs software rendering? apprarently canvas + color matrix is hardware and faster, why?
             * */
            // TODO: preprocess image here for better OCR results
            // ImgPreprocessor ipp = new ImgPreprocessor(base);

            mainWindow.setImageDrawable(new BitmapDrawable(getResources(), base));

            testButton.setVisibility(View.GONE);
            solv.setVisibility(View.VISIBLE);
        }
    });

    private Bitmap scaleToSize(Bitmap input, int maxDim, boolean filter) {

        double ratio;
        int newMinorDim;

        if (input.getWidth() > input.getHeight()) {

            ratio = (1.0 * input.getHeight()) / input.getWidth();
            newMinorDim = (int)Math.round(ratio * maxDim);

            return Bitmap.createScaledBitmap(input, maxDim, newMinorDim, filter);
        } else {

            ratio = (1.0 * input.getWidth()) / input.getHeight();
            newMinorDim = (int)Math.round(ratio * maxDim);

            return Bitmap.createScaledBitmap(input, newMinorDim, maxDim, filter);
        }
    }

    private Bitmap deskew(Bitmap input) {

        int delta = 1;
        int lim = 10;
        int offset = -1 * lim;
        int[] scores = new int[2 * lim + 1];

        // Log.i("dg", "base dims: " + input.getWidth() + ", " + input.getHeight());

        for (int i = 0; i < scores.length; i++) {

            scores[i] = getScore(input, (float)(epi * (offset + i)));
        }

        float correctionAngle = (float)(epi * getCA(scores, offset));

        Log.i("best angle", "img should rotate: " + correctionAngle);

        return rotateBitmap(input, correctionAngle);
    }

    private float getCA(int[] scores, int offset) {

        int maxScore = 0;
        int maxAngle = 0;

        for (int i = 0; i < scores.length; i++) {

            Log.i("angle", ((float)(epi * (offset + i))) + ", " + scores[i]);

            if (scores[i] > maxScore) {

                maxScore = scores[i];
                maxAngle = offset + i;
            }
        }

        return maxAngle;
    }

    private Bitmap rotateBitmap(Bitmap input, float angle) {

        Matrix ma = new Matrix();
        ma.postRotate(angle);
        Bitmap res =  Bitmap.createBitmap(input, 0, 0, input.getWidth(), input.getHeight(), ma, true);
//        Log.i("dg", "new dims: " + res.getWidth() + ", " + res.getHeight());
        return res;
    }

    private int[] getHist(Bitmap input) {

        int[] res = new int[input.getHeight()];
        int rowSum;

        for (int i = 0; i < input.getHeight(); i++) {

            rowSum = 0;

            for (int j = 0; j < input.getWidth(); j++) {

                rowSum += input.getPixel(j, i) == Color.BLACK ? 1 : 0;
            }

            res[i] = rowSum;
        }

        return res;
    }

    private int getScore(Bitmap input, float angle) {

        Bitmap res = rotateBitmap(input, angle);
        int[] selfHist = getHist(res);
        int score = 0;

        for (int i = 0; i < selfHist.length - 1; i++) {

            int diff = selfHist[i] - selfHist[i + 1];
            score += diff * diff;
        }

        return score;
    }

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {

            // TODO: launch cropper activity
            Uri dest = Uri.fromFile(new File(getCacheDir(), "whatever.jpg"));

            List<Uri> lst = new ArrayList<>();
            lst.add(result);
            lst.add(dest);

            mGetCropped.launch(lst);
        }
    });

    /**
     * TODO:
     * pass cropped board to BoardConverter
     * call imgToDigital for a matrix rep of given board
     * pass matrix board to solver n solve
     * get from solver a partial solution board
     * call digitalToImg to draw solutions on user screen
     * */
    private void solveGivenBoard() {

        TextRecognizer rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        InputImage img = InputImage.fromBitmap(base, 0);

        Task<Text> result =
                rec.process(img)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                // Task completed successfully
                                // ...

//                                base = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.RGB_565);
//                                Canvas canvas = new Canvas(base);
//
//                                canvas.drawBitmap(src, 0, 0, null);

                                Canvas canvas = new Canvas(base);
                                mainWindow.setImageDrawable(new BitmapDrawable(getResources(), base));

                                // init BoardConverter
                                BoardConverter brdConv = new BoardConverter(visionText, canvas, base);

                                // get digital board
                                int[][] userBoard = brdConv.imgToDigitalBoard();

                                brdConv.printBoard(userBoard);

                                // write immediately to screen to check
                                brdConv.digitalToImgBoard(userBoard);

                                // init engine
                                SudokuSolverV3 solver = new SudokuSolverV3(userBoard);

                                // solve board
                                int[][] solBoard = solver.solve();

                                brdConv.printBoard(solBoard);

                                // draw sol to user screen by passing sol to converter
                                brdConv.digitalToImgBoard(solBoard);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                        e.printStackTrace();
                                    }
                                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainWindow = (ImageView) findViewById(R.id.mainWindow);
        testButton = (Button) findViewById(R.id.initOcrTest);
        solv = (ImageButton) findViewById(R.id.imageButton);
        solv.setVisibility(View.GONE);

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mGetContent.launch("image/*");
            }
        });

        solv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                solveGivenBoard();
            }
        });
    }
}