package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.utils.collections.UpdatableValue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HotTree {

    private HotTree left;
    private HotTree right;
    private HotTree parent;
    private String name;
    private String operator;
    private UpdatableValue<Integer> occurrences;

    private int color;
    private static final String[] colors = {"\u001B[0m", "\u001B[36m", "\u001B[35m", "\u001B[34m", "\u001B[33m", "\u001B[32m", "\u001B[31m"};
    private static final String colorReset = "\u001B[0m";

    public HotTree(String name) {
        this.name = name;
        occurrences = new UpdatableValue<>(0);
        operator = "";
        color = 0;
    }

    public HotTree(String name, HotTree parent) {
        this.name = name;
        this.parent = parent;
        occurrences = new UpdatableValue<>(0);
        operator = "";
        color = (parent.getColor() + 1) % colors.length;
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

    public String getOperator() { return operator; }

    protected int getColor() {
        return color;
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

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(colors[color]);
        str.append("\n").append(name).append(": ").append(occurrences.current()).append(" (+")
                .append(occurrences.current() - occurrences.was()).append(")");
        str.append(colorReset);
        if (left != null) {
            str.append(Stream.of(left.toString().split("\n"))
                    .collect(Collectors.joining("\n  ")));
        }
        if (!operator.equals("")) {
            str.append(colors[(color + 1)% colors.length]).append("\n  ").append(operator).append(colorReset);

            if (right != null) {
                str.append(Stream.of(right.toString().split("\n"))
                        .collect(Collectors.joining("\n  ")));
            } else {
                str.append(colors[(color + 1)% colors.length]).append("\n  XXX").append(colorReset);
            }
        }
        return str.toString();
    }
}
