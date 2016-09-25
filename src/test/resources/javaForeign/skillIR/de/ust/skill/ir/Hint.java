package de.ust.skill.ir;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @see SKilL §5.2
 * @author Timm Felden
 */
public class Hint {

    public static enum Type {
        // note: names need to be lowercase, because this enum will be accessed
        // using the valueOf method
        owner, provider, removerestrictions, constantmutator, mixin, flat, unique, pure, distributed, ondemand,
        monotone, readonly, ignore, hide, pragma;
    }

    // unique hints
    public static Hint constantMutator = new Hint(Type.constantmutator);
    public static Hint distributed = new Hint(Type.distributed);
    public static Hint flat = new Hint(Type.flat);
    public static Hint hide = new Hint(Type.hide);
    public static Hint ignore = new Hint(Type.ignore);
    public static Hint mixin = new Hint(Type.mixin);
    public static Hint monotone = new Hint(Type.monotone);
    public static Hint ondemand = new Hint(Type.ondemand);
    public static Hint pure = new Hint(Type.pure);
    public static Hint readonly = new Hint(Type.readonly);
    public static Hint removeRestrictions = new Hint(Type.removerestrictions);
    public static Hint unique = new Hint(Type.unique);

    public Type type;

    public Type type() {
        return type;
    }

    public List<Name> arguments;

    public List<Name> arguments() {
        return arguments;
    }

    /**
     * public to ensure unique hints for those that do not take parameters
     */
    public Hint(Type type, List<Name> args) {
        this.type = type;
        this.arguments = args;

    }

    public Hint(Type type) {
        this(type, Collections.<Name> emptyList());
    }

    public static Hint get(Type type, List<Name> args) throws ParseException {
        switch (type) {
        case constantmutator:
            return constantMutator;
        case distributed:
            return distributed;
        case flat:
            return flat;
        case hide:
            return hide;
        case ignore:
            return ignore;
        case mixin:
            return mixin;
        case monotone:
            return monotone;
        case ondemand:
            return ondemand;
        case owner:
            return new Hint(type, args);
        case pragma:
            return new Hint(type, args);
        case provider:
            return new Hint(type, args);
        case pure:
            return pure;
        case readonly:
            return readonly;
        case removerestrictions:
            return removeRestrictions;
        case unique:
            return unique;
        default:
            throw new ParseException("unknown hint type: " + type);
        }
    }

    /**
     * checks that the argument declaration has no illegal arguments
     * 
     * @throws ParseException
     *             if there is any illegal usage of a hint
     */
    static void checkDeclaration(UserType d, Set<Hint> hints) throws ParseException {
        if (hints.contains(distributed))
            throw new ParseException("Illegal hint !distributed on type declaration " + d);
        if (hints.contains(ondemand))
            throw new ParseException("Illegal hint !lazy on type declaration " + d);

        if (hints.contains(monotone) && d != d.getBaseType())
            throw new ParseException(
                    "The hint !monotone can only be used on base types, because it is inherited anyway: " + d);
        if (hints.contains(readonly) && d != d.getBaseType())
            throw new ParseException(
                    "The hint !readOnly can only be used on base types, because it is inherited anyway: " + d);
    }

    /**
     * checks that the argument field has legal hints only
     * 
     * @throws ParseException
     *             if there is any illegal usage of a hint
     */
    static void checkField(FieldLike field, Set<Hint> hints) throws ParseException {

        if (hints.contains(unique))
            throw new ParseException("Illegal hint !unique on field declaration " + field);

        if (hints.contains(pure))
            throw new ParseException("Illegal hint !pure on field declaration " + field);

        if (hints.contains(monotone))
            throw new ParseException("Illegal hint !monotone on field declaration " + field);

        if (hints.contains(readonly))
            throw new ParseException("Illegal hint !readOnly on field declaration " + field);
    }
}
