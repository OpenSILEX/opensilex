//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.graphimport.preview.NodeState;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What the creation step would do, step by step, in the order of the writers: for each, the resources
 * ready to create, those blocked and why, and those the instance already holds.
 * <p>
 * It is worked out from the preview, which compares the files with the instance: once a step has written
 * its resources, the preview read afresh finds them, and they read as done. A step can so be run again,
 * or resumed after a failure, without creating anything twice.
 *
 * @param steps       one per writer, in the order the writers are given
 * @param unsupported for the resources to create that no writer creates, how many of each class
 * @author Arnaud Charleroy
 */
public record CreationPlan(List<Step> steps, Map<URI, Integer> unsupported) {

    public CreationPlan {
        steps = List.copyOf(steps);
        unsupported = Map.copyOf(unsupported);
    }

    /** Where a step stands. */
    public enum Status {
        /** Some resources are ready to create. */
        READY,
        /** Resources are to create, none can be yet. */
        BLOCKED,
        /** Everything this step creates exists. */
        DONE,
        /** The files hold nothing for this step. */
        EMPTY
    }

    /** A resource that cannot be created yet, and why. */
    public record Blocked(NodeComparison node, ReportMessage reason) {
    }

    /**
     * @param ready   the resources it would create now
     * @param blocked the resources it creates that cannot be created yet
     * @param done    how many of its resources the instance already holds unchanged
     */
    public record Step(ResourceWriter writer, Status status, List<NodeComparison> ready, List<Blocked> blocked,
                       int done) {

        public Step {
            ready = List.copyOf(ready);
            blocked = List.copyOf(blocked);
        }

        public String name() {
            return writer.step();
        }
    }

    /**
     * The plan with the resources the writers find already there moved from ready to done: those the
     * preview cannot find by name.
     */
    public CreationPlan withExisting(Map<String, Set<NodeComparison>> existing) {
        List<Step> adjusted = new ArrayList<>();
        for (Step step : steps) {
            Set<NodeComparison> there = existing.getOrDefault(step.name(), Set.of());
            if (there.isEmpty()) {
                adjusted.add(step);
                continue;
            }
            List<NodeComparison> ready = step.ready().stream().filter(node -> !there.contains(node)).toList();
            int done = step.done() + step.ready().size() - ready.size();
            Status status = !ready.isEmpty() ? Status.READY
                    : !step.blocked().isEmpty() ? Status.BLOCKED
                    : Status.DONE;
            adjusted.add(new Step(step.writer(), status, ready, step.blocked(), done));
        }
        return new CreationPlan(adjusted, unsupported);
    }

    public Optional<Step> step(String name) {
        return steps.stream().filter(step -> step.name().equals(name)).findFirst();
    }

    public static CreationPlan of(List<NodeComparison> nodes, List<ResourceWriter> writers, WriteContext context) {
        Map<NodeComparison, ResourceWriter> writerOf = new LinkedHashMap<>();
        Map<URI, Integer> unsupported = new LinkedHashMap<>();
        for (NodeComparison node : nodes) {
            Optional<ResourceWriter> writer = writerOf(node, writers, context);
            if (writer.isPresent()) {
                writerOf.put(node, writer.get());
            } else if (node.state() == NodeState.CREATED && node.classUri() != null) {
                unsupported.merge(node.classUri(), 1, Integer::sum);
            }
        }

        Map<ResourceWriter, List<NodeComparison>> candidates = new LinkedHashMap<>();
        Map<ResourceWriter, List<Blocked>> blocked = new LinkedHashMap<>();
        Map<ResourceWriter, Integer> done = new HashMap<>();
        writers.forEach(writer -> {
            candidates.put(writer, new ArrayList<>());
            blocked.put(writer, new ArrayList<>());
        });
        writerOf.forEach((node, writer) -> {
            Optional<ReportMessage> reason = firstReason(node, writer, context);
            if (node.state() == NodeState.UNCHANGED) {
                done.merge(writer, 1, Integer::sum);
            } else if (reason.isPresent()) {
                blocked.get(writer).add(new Blocked(node, reason.get()));
            } else {
                candidates.get(writer).add(node);
            }
        });

        Set<NodeComparison> ready = new LinkedHashSet<>();
        candidates.values().forEach(ready::addAll);
        // A resource pointing to a row still to create waits for it: a row another step creates must be
        // created first, a row of the same step must be ready too. And a resource that other resources of
        // its step point to waits for them, since they are created together: a factor without its levels
        // could not be given them afterwards.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (NodeComparison node : List.copyOf(ready)) {
                Optional<ReportMessage> reason = waitsFor(node, writerOf, ready, context)
                        .or(() -> waitedForBy(node, writerOf, ready));
                if (reason.isPresent()) {
                    ready.remove(node);
                    blocked.get(writerOf.get(node)).add(new Blocked(node, reason.get()));
                    changed = true;
                }
            }
        }

        List<Step> steps = new ArrayList<>();
        for (ResourceWriter writer : writers) {
            List<NodeComparison> stepReady = candidates.get(writer).stream().filter(ready::contains).toList();
            List<Blocked> stepBlocked = blocked.get(writer);
            int stepDone = done.getOrDefault(writer, 0);
            Status status = !stepReady.isEmpty() ? Status.READY
                    : !stepBlocked.isEmpty() ? Status.BLOCKED
                    : stepDone > 0 ? Status.DONE
                    : Status.EMPTY;
            steps.add(new Step(writer, status, stepReady, stepBlocked, stepDone));
        }
        return new CreationPlan(steps, unsupported);
    }

    private static Optional<ResourceWriter> writerOf(NodeComparison node, List<ResourceWriter> writers,
                                                     WriteContext context) {
        if (node.classUri() == null) {
            return Optional.empty();
        }
        return writers.stream()
                .filter(writer -> writer.rootClasses().stream().anyMatch(root -> node.classUri().equals(root)
                        || context.catalog().isA(node.classUri(), root)))
                .findFirst();
    }

    /** Why a resource of the step cannot be created, looking at the resource alone. */
    private static Optional<ReportMessage> firstReason(NodeComparison node, ResourceWriter writer,
                                                       WriteContext context) {
        return switch (node.state()) {
            case UNCHANGED -> Optional.empty();
            case COMPLETED -> Optional.of(CreationMessages.completing());
            case CONFLICT -> Optional.of(CreationMessages.conflict());
            case CREATED -> node.toFix() ? Optional.of(CreationMessages.toFix()) : writer.whyNot(node, context);
        };
    }

    /** A resource of the same step, not ready, that points to this one; empty when none. */
    private static Optional<ReportMessage> waitedForBy(NodeComparison node, Map<NodeComparison, ResourceWriter> writerOf,
                                                       Set<NodeComparison> ready) {
        String key = node.node().getKey();
        return writerOf.entrySet().stream()
                .filter(other -> other.getValue() == writerOf.get(node) && !ready.contains(other.getKey())
                        && other.getKey().state() == NodeState.CREATED
                        && NodeValues.of(other.getKey()).rowsPointedTo().contains(key))
                .findFirst()
                .map(other -> CreationMessages.pointedByBlocked(other.getKey().node().getName()));
    }

    /** What a resource waits for among the rows still to create it points to; empty when nothing. */
    private static Optional<ReportMessage> waitsFor(NodeComparison node, Map<NodeComparison, ResourceWriter> writerOf,
                                                    Set<NodeComparison> ready, WriteContext context) {
        ResourceWriter own = writerOf.get(node);
        for (String key : NodeValues.of(node).rowsPointedTo()) {
            NodeComparison target = context.nodes().get(key);
            if (target == null || target.state() != NodeState.CREATED) {
                continue;
            }
            ResourceWriter other = writerOf.get(target);
            String name = target.node().getName();
            if (other == null) {
                return Optional.of(CreationMessages.targetNotCreated(name));
            }
            if (other != own) {
                return Optional.of(CreationMessages.targetFirst(name, other.step()));
            }
            if (!ready.contains(target)) {
                return Optional.of(CreationMessages.targetBlocked(name));
            }
        }
        return Optional.empty();
    }
}
