package com.kpl.sudokusolvermk3;

import android.graphics.Point;

public class BoardElement {

    public int num;
    public Point loc;

    public BoardElement(int num, Point loc) {

        this.num = num;
        this.loc = loc;
    }
}
