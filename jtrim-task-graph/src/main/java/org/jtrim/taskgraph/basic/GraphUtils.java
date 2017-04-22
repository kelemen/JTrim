package org.jtrim.taskgraph.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

final class GraphUtils {
    public static <N> List<N> sortRecursively(
            DirectedGraph<N> graph,
            Collection<? extends N> rootNodes,
            Set<N> src) {
        ExceptionHelper.checkNotNullArgument(graph, "graph");
        ExceptionHelper.checkNotNullArgument(src, "src");

        List<N> result = new ArrayList<>(src.size());
        Set<N> visited = new HashSet<>();
        rootNodes.forEach((root) -> {
            ExceptionHelper.checkNotNullArgument(root, "rootNodess[?]");
            addNodesRecursively(graph, root, src, visited, result);
        });
        return result;
    }

    private static <N> void addNodesRecursively(
            DirectedGraph<N> graph,
            N root,
            Set<N> src,
            Set<N> visited,
            Collection<N> result) {

        if (!visited.add(root)) {
            return;
        }

        graph.getChildren(root).forEach((child) -> {
            addNodesRecursively(graph, child, src, visited, result);
        });

        if (src.contains(root)) {
            result.add(root);
        }
    }

    private GraphUtils() {
        throw new AssertionError();
    }
}
