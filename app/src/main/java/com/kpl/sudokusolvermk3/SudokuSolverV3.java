package com.kpl.sudokusolvermk3;

class SudokuSolverV3 {

    private SSEngine engine;

    public SudokuSolverV3(int[][] board) {

        this.engine = new SSEngine(board);
    }

    public void updateBoard(int[][] board) {

        this.engine = new SSEngine(board);
    }

    public int[][] solve() {

        return engine.solve();
    }
}
