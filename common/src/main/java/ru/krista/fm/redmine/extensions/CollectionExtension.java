package ru.krista.fm.redmine.extensions;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.krista.fm.redmine.exceptions.ExportServiceArgumentNullException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Component
public class CollectionExtension {

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Node<T> {
        private int level;
        private int levelCommon;
        private Node<T> parent;
        private T item;
        private List<Node<T>> nodes = new ArrayList<>();
        private List<Node<T>> hierarchy;

        public Node(int level, int levelCommon, Node<T> parent, T item) {
            this.level = level;
            this.levelCommon = levelCommon;
            this.parent = parent;
            this.item = item;
        }

        public List<Node<T>> getHierarchy() {
            var hierarchy = new ArrayList<Node<T>>();
            hierarchy.add(this);
            for (var node : nodes) {
                hierarchy.addAll(node.hierarchy);
            }
            return hierarchy;
        }

        /**
         * Возвращает все дочерние элементы со всех уровней иерархии
         */
        public List<Node<T>> descendants() {
            return descendants(true);
        }

        /**
         * Возвращает все дочерние элементы со всех уровней иерархии
         */
        public List<Node<T>> descendants(boolean withSelf) {
            var result = new ArrayList<Node<T>>();
            if (withSelf) result.add(this);
            for (var node : nodes) {
                result.addAll(node.descendants(true));
            }

            return result;
        }

        /**
         * Возвращает всех предков
         */
        public List<Node<T>> parents() {
            return parents(true);
        }

        /**
         * Возвращает всех предков
         */
        public List<Node<T>> parents(boolean withSelf) {
            var result = new ArrayList<Node<T>>();
            if (withSelf) result.add(this);
            if (parent != null) result.addAll(parent.parents());
            return result;
        }
    }

    public static <T> List<Node<T>> byHierarchy(List<T> source,
                                         Function<T, Boolean> startWith,
                                         BiFunction<T, T, Boolean> connectBy) throws ExportServiceArgumentNullException {
        return byHierarchy(source, startWith, connectBy, p -> true, null, 0);
    }

    public static <T> List<Node<T>> byHierarchy(List<T> source,
                                         Function<T, Boolean> startWith,
                                         BiFunction<T, T, Boolean> connectBy,
                                         Function<T, Boolean> filter) throws ExportServiceArgumentNullException {
        return byHierarchy(source, startWith, connectBy, filter, null, 0);
    }

    private static <T> List<Node<T>> byHierarchy(List<T> source,
                                          Function<T, Boolean> startWith,
                                          BiFunction<T, T, Boolean> connectBy,
                                          Function<T, Boolean> filter,
                                          Node<T> parent, int levelCommon) throws ExportServiceArgumentNullException {
        var level = parent == null ? 0 : parent.level + 1;
        if (source == null) throw new ExportServiceArgumentNullException("source");
        if (startWith == null) throw new ExportServiceArgumentNullException("startWith");
        if (connectBy == null) throw new ExportServiceArgumentNullException("connectBy");

        List<Node<T>> hierarchy = new ArrayList<>();
        var startWithList = source.stream().filter(startWith::apply).toList();
        for (T value : startWithList) {
            Node<T> newNode = null;

            if (filter.apply(value)) {
                newNode = new Node<>(level, levelCommon, parent, value);
                if (parent != null) parent.nodes.add(newNode);
                hierarchy.add(newNode);
            }

            var node = newNode != null ? newNode : parent;
            hierarchy.addAll(byHierarchy(
                    source, possibleSub -> connectBy.apply(value, possibleSub),
                    connectBy, filter,
                    node, levelCommon + 1));
        }

        return hierarchy;
    }
}
