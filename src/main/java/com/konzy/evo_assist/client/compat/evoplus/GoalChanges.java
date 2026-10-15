package com.konzy.evo_assist.client.compat.evoplus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Editing a field must not reset a running goal on each keystroke. */
public final class GoalChanges {
    private record Target(Supplier<String> get, Consumer<String> set, UnaryOperator<String> normalize,
                          Predicate<String> resetsProgress) {}
    public record Change(String id, String previous, String next) {}
    private final Map<String, Target> targets = new LinkedHashMap<>();
    private final Map<String, String> drafts = new LinkedHashMap<>();

    public void register(String id, Supplier<String> get, Consumer<String> set,
                         UnaryOperator<String> normalize, Predicate<String> resetsProgress) {
        targets.put(id, new Target(get, set, normalize, resetsProgress));
    }

    public String get(String id) { return drafts.getOrDefault(id, targets.get(id).get().get()); }
    public String current(String id) { return targets.get(id).get().get(); }
    public String normalize(String id, String value) { return targets.get(id).normalize().apply(value); }
    public void edit(String id, String value) { drafts.put(id, normalize(id, value)); }
    public void discard(String id) { drafts.remove(id); }

    /** An explicitly confirmed reset applies immediately, without affecting other drafts. */
    public void reset(String id) {
        drafts.remove(id);
        Target target = targets.get(id);
        target.set().accept(target.normalize().apply("0"));
    }

    public void save(BiConsumer<List<Change>, Runnable> confirm, Runnable persisted) {
        List<Change> changes = new ArrayList<>();
        drafts.forEach((id, value) -> {
            Target target = targets.get(id);
            String previous = target.get().get();
            String normalized;
            try { normalized = target.normalize().apply(previous); }
            catch (IllegalArgumentException invalidLegacyValue) { normalized = previous; }
            if (!normalized.equals(value)) changes.add(new Change(id, previous, value));
        });
        drafts.clear();
        Runnable apply = () -> {
            changes.forEach(change -> targets.get(change.id()).set().accept(change.next()));
            persisted.run();
        };
        if (changes.stream().anyMatch(change -> targets.get(change.id()).resetsProgress().test(change.next()))) {
            // Non-goal settings were already changed through bind; cancelling a goal must still save them.
            persisted.run();
            confirm.accept(List.copyOf(changes), apply);
        } else apply.run();
    }
}
