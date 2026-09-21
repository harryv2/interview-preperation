package lld.ruleenginejava.strategies.actions;

import lld.ruleenginejava.entity.Facts;

@FunctionalInterface
public interface Action {
    Effect execute(Facts facts);
}
