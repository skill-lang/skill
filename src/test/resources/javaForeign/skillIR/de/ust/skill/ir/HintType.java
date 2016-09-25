package de.ust.skill.ir;

public enum HintType {
    // note: names need to be lowercase, because this enum will be accessed
    // using the valueOf method
    owner, provider, removerestrictions, constantmutator, mixin, flat, unique, pure, distributed, ondemand,
    monotone, readonly, ignore, hide, pragma;
}