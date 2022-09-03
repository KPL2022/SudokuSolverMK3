package com.kpl.sudokusolvermk3;

import java.util.*;

// Board Spot container class
public class Bspot {

    public boolean filled;

    // could probs better integrate this field and above...
    public int selfNum;
    public List<Integer> cPool;
    public int rId;
    public int cId;

    public Bspot(boolean filled, List<Integer> cPool, int rId, int cId, int selfNum) {

        this.filled = filled;
        this.cPool = cPool;
        this.rId = rId;
        this.cId = cId;
        this.selfNum = selfNum;
    }
}
