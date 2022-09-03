package com.kpl.sudokusolvermk3;

import java.util.*;

// collection of spots (filled/empty) for row,col, or sq
public class SpotCol {

    // filled spots in this "constraint grp" e {row, col, sq}
    public Set<Bspot> filled;

    // empty spots ~~~
    public Set<Bspot> empty;

    // col of empty spots idx'ed by candidate number n e {1..9}
    public Map<Integer, List<Bspot>> sPool;

    public SpotCol() {

        this(null, null, null);
    }

    public SpotCol(Set<Bspot> filled, Set<Bspot> empty, Map<Integer, List<Bspot>> sPool) {

        this.filled = filled;
        this.empty = empty;
        this.sPool = sPool;
    }
}
