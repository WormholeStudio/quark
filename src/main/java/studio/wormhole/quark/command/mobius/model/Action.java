package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Action {

    DEPOSIT("MarketScript::deposit"),
    WITHDRAW("MarketScript::withdraw"),
    BORROW("MarketScript::borrow"),
    REPAY("MarketScript::repay"),
    ;

    private String function;

    public static Action fromString(String a) {
        return Arrays.stream(Action.values()).filter(s -> s.name().equalsIgnoreCase(a)).findAny().orElseThrow(() -> new RuntimeException("not support action"));
    }
}
