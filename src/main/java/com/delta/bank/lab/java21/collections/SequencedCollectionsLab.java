package com.delta.bank.lab.java21.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;

public class SequencedCollectionsLab {

    public <T> T first(SequencedCollection<T> collection) {
        Objects.requireNonNull(collection, "collection must not be null");
        return collection.getFirst();
    }

    public <T> T last(SequencedCollection<T> collection) {
        Objects.requireNonNull(collection, "collection must not be null");
        return collection.getLast();
    }

    public <T> SequencedCollection<T> reversedView(
            SequencedCollection<T> collection
    ) {
        Objects.requireNonNull(collection, "collection must not be null");
        return collection.reversed();
    }

    public <T> List<T> reversedCopy(SequencedCollection<T> collection) {
        return new ArrayList<>(reversedView(collection));
    }

    public <T> T first(SequencedSet<T> set) {
        Objects.requireNonNull(set, "set must not be null");
        return set.getFirst();
    }

    public <T> T last(SequencedSet<T> set) {
        Objects.requireNonNull(set, "set must not be null");
        return set.getLast();
    }

    public <K, V> Map.Entry<K, V> firstEntry(SequencedMap<K, V> map) {
        Objects.requireNonNull(map, "map must not be null");
        return map.firstEntry();
    }

    public <K, V> Map.Entry<K, V> lastEntry(SequencedMap<K, V> map) {
        Objects.requireNonNull(map, "map must not be null");
        return map.lastEntry();
    }

    public <K, V> SequencedMap<K, V> reversedMapView(
            SequencedMap<K, V> map
    ) {
        Objects.requireNonNull(map, "map must not be null");
        return map.reversed();
    }
}