package com.konzy.evo_assist.client.compat.evoplus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GoalChangesChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var editor = new GoalChanges();
        var target = new AtomicReference<>("100.00");
        var other = new AtomicReference<>("50");
        var resets = new AtomicInteger();
        var saves = new AtomicInteger();
        var dialogs = new ArrayList<List<GoalChanges.Change>>();
        var apply = new AtomicReference<Runnable>();
        editor.register("money", target::get, value -> { target.set(value); resets.incrementAndGet(); },
                value -> new BigDecimal(value).stripTrailingZeros().toPlainString(), value -> true);
        editor.register("blocks", other::get, other::set, value -> value, value -> false);
        editor.edit("money", "5"); editor.edit("money", "500"); editor.edit("blocks", "250");
        check(target.get().equals("100.00") && resets.get() == 0, "Keystrokes leave active goal progress untouched");
        check(editor.get("money").equals("500"), "Field displays draft value");
        editor.save((changes, confirmed) -> { dialogs.add(changes); apply.set(confirmed); }, saves::incrementAndGet);
        check(dialogs.size() == 1 && dialogs.getFirst().size() == 2 && target.get().equals("100.00")
                && other.get().equals("50"), "Close requests one confirmation before applying any goal changes");
        check(editor.get("money").equals("100.00"), "Cancel or Escape discards the draft and preserves the existing target");
        check(saves.get() == 1, "Unrelated source and interface settings persist even if the goal change is cancelled");
        editor.edit("money", "100");
        editor.save((changes, confirmed) -> { throw new AssertionError("Equal numeric goal asked for confirmation"); }, saves::incrementAndGet);
        check(resets.get() == 0, "Different formatting of the same target never resets progress");
        apply.get().run();
        check(target.get().equals("500") && other.get().equals("250") && resets.get() == 1,
                "Confirmation applies the complete change once");
        editor.edit("blocks", "300");
        editor.discard("blocks");
        editor.save((changes, confirmed) -> { throw new AssertionError("Discarded draft was applied"); }, saves::incrementAndGet);
        check(other.get().equals("250"), "Resetting an individual goal cannot restore its discarded draft on close");
        editor.edit("blocks", "400");
        editor.save((changes, confirmed) -> { throw new AssertionError("Fresh goal needs no confirmation"); }, saves::incrementAndGet);
        check(other.get().equals("400") && saves.get() == 5, "Targets without running progress apply and persist on close");
        editor.edit("money", "700");
        editor.edit("blocks", "900");
        editor.reset("money");
        check(target.get().equals("0") && editor.get("money").equals("0") && editor.get("blocks").equals("900"),
                "Confirmed reset applies immediately and preserves the other goal draft");
        editor.save((changes, confirmed) -> { throw new AssertionError("Reset draft came back on close"); }, saves::incrementAndGet);
        check(target.get().equals("0") && other.get().equals("900"), "Closing after reset cannot restore the old target");
        System.out.println("Goal editing checks passed: staged changes, confirmation, cancel, numeric equality and persistence.");
    }
}
