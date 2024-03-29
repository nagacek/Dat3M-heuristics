package com.dat3m.dartagnan.utils.equivalence;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.stream.Collectors;

public interface Equivalence<T> {

    EquivalenceClass<T> getEquivalenceClass(T x);
    Set<? extends EquivalenceClass<T>> getAllEquivalenceClasses();

    default boolean areEquivalent(T a, T b) {
        return getEquivalenceClass(a).equals(getEquivalenceClass(b));
    }

    default T getRepresentative(T x) {
        return getEquivalenceClass(x).getRepresentative();
    }
    default Set<T> getAllRepresentatives() {
        return getAllEquivalenceClasses().stream().map(EquivalenceClass::getRepresentative)
                .collect(Collectors.toSet());
    }

    default Set<? extends EquivalenceClass<T>> getNonTrivialClasses() {
        return getAllEquivalenceClasses().stream().filter(c -> c.size() > 1)
                .collect(Collectors.toCollection(Sets::newIdentityHashSet));
    }
}
