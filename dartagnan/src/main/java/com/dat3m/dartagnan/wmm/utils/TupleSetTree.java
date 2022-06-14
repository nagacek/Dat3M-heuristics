package com.dat3m.dartagnan.wmm.utils;

public class TupleSetTree {
    private TupleSet difference;
    private TupleSetTree r1;
    private TupleSetTree r2;

    public TupleSetTree(TupleSet difference) {
        this.difference = difference;
    }

    public TupleSetTree(TupleSet difference, TupleSetTree r1, TupleSetTree r2) {
        this.difference = difference;
        this.r1 = r1;
        this.r2 = r2;
    }

    public void merge(TupleSetTree tree) {
        this.difference.addAll(tree.getDifference());
        if (r1 != null) {
            r1.merge(tree.getR1());
        } else {
            r1 = tree.getR1();
        }
        if (r2 != null) {
            r2.merge(tree.getR2());
        } else {
            r2 = tree.getR2();
        }
    }

    public TupleSet getDifference() {
        return difference;
    }

    public TupleSetTree getR1() {
        return r1;
    }

    public TupleSetTree getR2() {
        return r2;
    }

    public void setR1(TupleSetTree r1) {
        this.r1 = r1;
    }

    public void setR2(TupleSetTree r2) {
        this.r2 = r2;
    }
}
