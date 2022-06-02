package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.utils.collections.UpdatableValue;

public class HotTree {

    private HotTree left;
    private HotTree right;
    private HotTree parent;
    private String name;
    private String operator;
    private UpdatableValue<Integer> occurrences;

    public HotTree(String name) {
        this.name = name;
        occurrences = new UpdatableValue<>(0);
    }

    public HotTree(String name, HotTree parent) {
        this.name = name;
        this.parent = parent;
        occurrences = new UpdatableValue<>(0);
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setLeft(String name) {
        this.left = new HotTree(name, this);
    }

    public void setRight(String name) {
        this.right = new HotTree(name, this);
    }

    public HotTree getLeft() {
        return left;
    }

    public HotTree getRight() {
        return right;
    }

    public HotTree getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void increment() {
        occurrences.setCurrent(occurrences.current() + 1);
    }

    public void update() {
        occurrences.update();
        if (left != null) {
            left.update();
        }
        if (right != null) {
            right.update();
        }
    }
}
