package com.konzy.evo_assist.client.compat.evoplus;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import ru.dargen.evoplus.api.ui.Align;
import ru.dargen.evoplus.api.ui.Ui;
import ru.dargen.evoplus.api.ui.UiContainer;

/** An inline goal input with a reset icon that remains visible at the default value. */
public final class GoalTargetControl {
    private GoalTargetControl() {}

    public static UiContainer create(Ui ui, String resetTooltip, boolean amount,
                                     Supplier<String> get, Consumer<String> edit,
                                     Predicate<String> valid, Runnable reset) {
        var input = ui.input(get.get(), "0").size(amount ? 100 : 60, 16).maxLength(amount ? 32 : 9);
        // Allow incomplete input while typing a decimal; only complete valid values become drafts.
        input.filter(value -> amount ? value.matches("\\d*(?:\\.\\d*)?[KMBTQkmbtq]?")
                : value.chars().allMatch(Character::isDigit));
        boolean[] refreshing = {false};
        Runnable refresh = () -> {
            if (input.isFocused()) return;
            String value = get.get();
            if (value.equals(input.getValue())) return;
            refreshing[0] = true;
            try { input.value(value); }
            finally { refreshing[0] = false; }
        };
        input.onChange(value -> {
            if (!refreshing[0] && valid.test(value)) edit.accept(value);
        });
        input.onSubmit(value -> {
            if (valid.test(value)) edit.accept(value);
            input.focus(false);
            refresh.run();
        });
        input.onTick(refresh);
        var button = ui.iconButton("evo-plus:textures/gui/icon/setting/reset.png", 14, resetTooltip, () -> {
            input.focus(false);
            reset.run();
            refresh.run();
        });
        return ui.row().size((amount ? 100 : 60) + 18, 18).spacing(4).align(Align.CENTER).add(button, input);
    }
}
