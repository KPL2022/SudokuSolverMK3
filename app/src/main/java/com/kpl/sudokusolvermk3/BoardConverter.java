package com.kpl.sudokusolvermk3;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;

public class BoardConverter {

    private static final boolean ROW_TRANSFORM = true;
    private static final boolean COL_TRANSFORM = false;

    private Canvas canvas;
    private Bitmap base;
    private List<BoardElement> givens;
    private int hBlockSize;
    private int vBlockSize;
    private int hOrigin;
    private int vOrigin;

    public BoardConverter(Text imgParse, Canvas canvas, Bitmap base) {

        this.canvas = canvas;
        this.base = base;

        givens = new ArrayList<>();

        calibrateBoardDimensions();

        for (Text.TextBlock block : imgParse.getTextBlocks()) {

            for (Text.Line line : block.getLines()) {

                filterElements(line.getElements());
            }
        }
    }

    private Point getPtFromBnds(Point[] bounds) {

        Point res = new Point();

        Point topLeft = bounds[0];
        Point botRight = bounds[2];

        res.x = (botRight.x + topLeft.x) / 2;
        res.y = (botRight.y + topLeft.y) / 2;

        return res;
    }

    private void filterElements(List<Text.Element> elements) {

        // String elementText = element.getText();
        // Point[] elementCornerPoints = element.getCornerPoints();

        for (Text.Element element : elements) {

            char[] eBdy = element.getText().toCharArray();
            Point loc = getPtFromBnds(element.getCornerPoints());
            loc.x -= hBlockSize;

            Point[] corners = element.getCornerPoints();
            Log.i("dg", element.getText() + ": " + corners[0] + ", " + corners[1] + ", " + corners[2] + ", " + corners[3]);

            for (int i = 0; i < eBdy.length; i++) {

                if (eBdy[i] != '|') {

                    loc.x += hBlockSize;
                }

                if (Character.isDigit(eBdy[i])) {

                    givens.add(new BoardElement(Character.getNumericValue(eBdy[i]), new Point(loc)));
                }
            }

//            Log.i("com.kpl.sudokusolvermk3", element.getText());
//            Log.i("com.kpl.sudokusolvermk3", String.valueOf(element.getCornerPoints()[0]));
//            Log.i("com.kpl.sudokusolvermk3", "over");
        }

//        Log.i("com.kpl.sudokusolverv3", "given size is: " + givens.size());
//
//        for (BoardElement be : givens) {
//
//            Log.i("com.kpl.sudokusolverv3", String.valueOf(be.num));
//        }

        /**
         * a few things can happen with text recognition
         *
         * 1. numbers get stuck together, so ins "4 9" we get "49"
         *      - if number extracted from string is > 9, need to split
         *      - problem is, how to find location of 2nd number?
         *           - need "blockSize" data...somehow revisit these nodes? if i see them b4 i get block size
         *
         * 2. input is not a number, so edge of board is common, like "|4" is really only "4"
         *      - filter out the edge "|"
         *      - the edge is actually useful, can kinda get info about the board itself...but not enough
         *
         * 3. input is in sequence, like "7 8 5" three times
         *      - i need to know when the element contains multiple board elements (X of them)
         *      - then i need to iterate thru the INPUT elements X times, like
         *           - "7 8 5", "7 8 5", "7 8 5"
         *           - but i gotta prog thru 7, 8, and 5 myself
         *
         * 4. any combination of 1, 2, and 3
         * */
    }

    // hmm..very basic estimations...
    private void calibrateBoardDimensions() {

        int width = base.getWidth();
        int height = base.getHeight();

        int hMargin = width / 50;  // assume 2% e dims whitespace outside frame from user cropping
        int vMargin = height / 50;

        width = width - 2 * hMargin;
        height = height - 2 * vMargin;

        hBlockSize = width / 9;
        hOrigin = hBlockSize / 2 + hMargin;
        vBlockSize = height / 9;
        vOrigin = vBlockSize / 2 + vMargin;
    }

    public int[][] imgToDigitalBoard() {

        // 9x9 -> dims e sudoku board
        int[][] res = new int[9][9];
        int r;
        int c;

        // Log.i("com.kpl.sudokusolverv3", "given size is now: " + givens.size());

        for (BoardElement be : givens) {

            r = pixToDigLoc(be.loc.y, ROW_TRANSFORM);
            c = pixToDigLoc(be.loc.x, COL_TRANSFORM);

            Log.i("be", "(" + r + ", " + c + "): " + be.num);
            Log.i("be", "(" + be.loc.y + ", " + be.loc.x + "): " + be.num);
            res[r][c] = be.num;
        }

        return res;
    }

    private int pixToDigLoc(int pixLoc, boolean transform) {

        int margin;

        if (transform == COL_TRANSFORM) {

            margin = hBlockSize / 4;

            return (int)(((pixLoc - hOrigin) + margin) / hBlockSize);
        } else {

            margin = vBlockSize / 4;

            return (int)(((pixLoc - vOrigin) + margin) / vBlockSize);
        }
    }

    public void digitalToImgBoard(int[][] inputBoard) {

        Paint paint = getTextPaint();

        for (int i = 0; i < inputBoard.length; i++) {

            int vPos = (vOrigin + i * vBlockSize);

            for (int j = 0; j < inputBoard[0].length; j++) {

                if (inputBoard[i][j] != 0) {

                    canvas.drawText(String.valueOf(inputBoard[i][j]), (hOrigin + j * hBlockSize), vPos, paint);
                }
            }
        }
    }

    private Paint getTextPaint() {

        Paint res = new Paint();
        res.setColor(Color.BLUE);
        res.setTextSize(getTextSize());

        return res;
    }

    private float getTextSize() {

        // hmm...lol very questionable setup
        // but with maxDim lim on input img, constants should be ok
        return 20;
    }

    public void printBoard(int[][] board) {

        StringBuilder strbrd = new StringBuilder();

        Log.i("com.kpl.sudokusolverv3", "++++++++++++++++++++++++++++++++");

        for (int i = 0; i < board.length; i++) {

            if (i % 3 == 0) {

                Log.i("com.kpl.sudokusolverv3", "----------------------");
            }

            for (int j = 0; j < board[0].length; j++) {

                if (j % 3 == 0) {

                    strbrd.append('|');
                }

                strbrd.append(board[i][j] + " ");
            }

            strbrd.append('|');

            Log.i("com.kpl.sudokusolverv3", strbrd.toString());
            strbrd = new StringBuilder();
        }

        Log.i("com.kpl.sudokusolverv3", "----------------------");
    }
}
