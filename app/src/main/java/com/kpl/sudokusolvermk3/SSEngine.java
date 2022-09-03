package com.kpl.sudokusolvermk3;

import java.util.*;

class SSEngine {

    private static final boolean FILLED = true;

    private int[][] board;

    private Map<Integer, SpotCol> rows;

    private Map<Integer, SpotCol> cols;

    private Map<Integer, SpotCol> sqs;

    Queue<Bspot> eSpots;

    public SSEngine(int[][] board) {

        this.board = board;

        // do init stuff here with the given board
        // assume proper form from client
        primeStateContainers();

        primeBoardState();

        integrateEmptySpots();
    }

    private void integrateEmptySpots() {

        int qSize = eSpots.size();
        Bspot cur;

        for (int i = 0; i < qSize; i++) {

            cur = eSpots.remove();
            integrateSpot(cur);
            eSpots.add(cur);
        }
    }

    private void integrateSpot(Bspot spot) {

        trimCandidates(spot);
        registerSelf(spot);
    }

    private int convLocSqId(int row, int col) {

        return 3 * (row / 3) + (col / 3);
    }

    private List<Bspot> getSpotPool(Map<Integer, SpotCol> container, int id, int plIdx) {

        if (!container.get(id).sPool.containsKey(plIdx)) {

            container.get(id).sPool.put(plIdx, new ArrayList<>());
        }

        return container.get(id).sPool.get(plIdx);
    }

    // register self to row, col, sq sPool idx by self candidates
    private void registerSelf(Bspot s) {

        List<Bspot> rSpPl;
        List<Bspot> cSpPl;
        List<Bspot> sqSpPl;

        for (Integer c : s.cPool) {

            rSpPl = getSpotPool(rows, s.rId, c);
            cSpPl = getSpotPool(cols, s.cId, c);
            sqSpPl = getSpotPool(sqs, convLocSqId(s.rId, s.cId), c);

            rSpPl.add(s);
            cSpPl.add(s);
            sqSpPl.add(s);
        }
    }

    private void trimCandidates(Bspot s) {

        Set<Bspot> rowFilled = rows.get(s.rId).filled;
        Set<Bspot> colFilled = cols.get(s.cId).filled;
        Set<Bspot> sqFilled = sqs.get(convLocSqId(s.rId, s.cId)).filled;

        Set<Integer> hasSeen = combine(rowFilled, colFilled, sqFilled);

        for (int i = 1; i < 10; i++) {

            if (!hasSeen.contains(i)) {

                s.cPool.add(i);
            }
        }
    }

    private Set<Integer> combine(Set<Bspot> rows, Set<Bspot> cols, Set<Bspot> sqs) {

        Set<Integer> res = new HashSet<>();

        bulkAdd(rows, res);
        bulkAdd(cols, res);
        bulkAdd(sqs, res);

        return res;
    }

    private void bulkAdd(Set<Bspot> a, Set<Integer> res) {

        for (Bspot b : a) {

            res.add(board[b.rId][b.cId]);
        }
    }

    private void primeBoardState() {

        this.eSpots = new LinkedList<>();
        Bspot tmp;

        // go thru board, for each spot, init filled or empty, add to specific row, col, sq
        // if empty, add to queue
        for (int i = 0; i < board.length; i++) {

            for (int j = 0; j < board[0].length; j++) {

                tmp = new Bspot(FILLED, new ArrayList<>(), i, j, -1);

                if (board[i][j] == 0) {
                    tmp.filled = false;
                    eSpots.add(tmp);

                    rows. get(i).empty.add(tmp);
                    cols.get(j).empty.add(tmp);
                    sqs.get(convLocSqId(i, j)).empty.add(tmp);
                } else {

                    // what was self num used for again?
                    tmp.selfNum = board[i][j];
                    rows.get(i).filled.add(tmp);
                    cols.get(j).filled.add(tmp);
                    sqs.get(convLocSqId(i, j)).filled.add(tmp);
                }
            }
        }
    }

    private void primeStateContainers() {

        rows = new HashMap<>();
        cols = new HashMap<>();
        sqs = new HashMap<>();

        initConstGrp(rows);
        initConstGrp(cols);
        initConstGrp(sqs);
    }

    private void initConstGrp(Map<Integer, SpotCol> a) {

        for (int i = 0; i < 9; i++) {

            SpotCol ttmp = new SpotCol(new HashSet<>(), new HashSet<>(), new HashMap<>());
            a.put(i, ttmp);
        }
    }

    // clean this class up xD
    // gives partial solution board back, only solution written, rest '0'
    public int[][] solve() {

        int[][] res = new int[board.length][board[0].length];

        int patience = 1000;

        while (eSpots.size() > 0 && patience > 0) {

            Bspot spot = eSpots.remove();

            updateCandidates(spot);

            List<Integer> cPool = spot.cPool;
            Iterator<Integer> itr = cPool.iterator();

            while (!spot.filled && itr.hasNext()) {

                Integer n = itr.next();

                if (isSolSelf(spot, n)) {

                    // update internal state
                    spot.filled = true;
                    spot.selfNum = n;
                    itr.remove();

                    // update board's knowledge about self
                    revFromEmpty(spot);
                    addToFilled(spot);

                    // write to solution board
                    res[spot.rId][spot.cId] = n;
                }
            }

            // System.out.println("spot at (" + spot.rId + ", " + spot.cId + ") is filled? " + spot.filled);

            if (spot.filled) {

                // update rest of candidates knowledge about self
                for (Integer g : cPool) {

                    revSelfCandSpPl(spot, g);
                }
            } else {

                // return spot to pq
                eSpots.add(spot);
            }
            patience--;
        }

        if (patience == 0) {

            while (eSpots.size() > 0) {

                Bspot lp = eSpots.remove();

                if (lp.rId == 0 && lp.cId == 3) {

                    System.out.println(lp.cPool.toString());
                }
            }
        }

        return res;
    }

    private void updateCandidates(Bspot b) {

        List<Integer> cPool = b.cPool;
        Iterator<Integer> itr = cPool.iterator();

        while (itr.hasNext()) {

            Integer c = itr.next();

            // check candidate validity, remove if filled alr
            if (isCandiFilled(b, c)) {

                revSelfCandSpPl(b, c);
                itr.remove();
            }
        }
    }

    private boolean isCandiFilled(Bspot b, int c) {

        return findNumInSet(c, rows.get(b.rId).filled) || findNumInSet(c, cols.get(b.cId).filled) || findNumInSet(c, sqs.get(convLocSqId(b.rId, b.cId)).filled);
    }

    private boolean findNumInSet(int n, Set<Bspot> s) {

        for (Bspot b : s) {

            if (b.selfNum == n) {

                return true;
            }
        }

        return false;
    }

    private void revSelfCandSpPl(Bspot b, int g) {

        List<Bspot> gRowSpPl = rows.get(b.rId).sPool.get(g);
        List<Bspot> gColSpPl = cols.get(b.cId).sPool.get(g);
        List<Bspot> gSqSpPl = sqs.get(convLocSqId(b.rId, b.cId)).sPool.get(g);

        gRowSpPl.remove(b);
        gColSpPl.remove(b);
        gSqSpPl.remove(b);
    }

    private boolean isSolSelf(Bspot b, int c) {

        if (b.cPool.size() == 1) return true;

        List<Bspot> cRowSpPl = rows.get(b.rId).sPool.get(c);
        List<Bspot> cColSpPl = cols.get(b.cId).sPool.get(c);
        List<Bspot> cSqSpPl = sqs.get(convLocSqId(b.rId, b.cId)).sPool.get(c);

        return cRowSpPl.size() == 1 || cColSpPl.size() == 1 || cSqSpPl.size() == 1;
    }

    private void revFromEmpty(Bspot b) {

        rows.get(b.rId).empty.remove(b);
        cols.get(b.cId).empty.remove(b);
        sqs.get(convLocSqId(b.rId, b.cId)).empty.remove(b);
    }

    private void addToFilled(Bspot b) {

        rows.get(b.rId).filled.add(b);
        cols.get(b.cId).filled.add(b);
        sqs.get(convLocSqId(b.rId, b.cId)).filled.add(b);
    }
}
