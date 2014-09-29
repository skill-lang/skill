package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A view onto another field.
 * 
 * @see SKilL V1.0 §6.?
 * @author Timm Felden
 */
// TODO ensure that all Fields works correctly here:)
public class View extends Field {

    final private Name typeName, field;

    public View(Name declaredIn, Field target, Type type, Name name, Comment comment) throws ParseException {
        super(type, name, target.isAuto(), comment, target.restrictions, unhide(target.hints));
        typeName = declaredIn;
        field = target.getName();
    }

    protected View(Name typeName, Name field, boolean auto, boolean isConstant, long constantValue, Name name,
            Type newType, Declaration declaredIn, ArrayList<Restriction> rs, HashSet<Hint> hs, Comment comment) {
        super(auto, isConstant, constantValue, name, newType, declaredIn, rs, hs, comment);
        this.typeName = typeName;
        this.field = field;
    }

    /**
     * removes a !hide hint if present
     */
    private static Collection<Hint> unhide(Set<Hint> hints) {
        if (hints.contains(Hint.hide)) {
            HashSet<Hint> rval = new HashSet<>(hints);
            rval.remove(Hint.hide);
            return rval;
        }
        return hints;
    }

    public Name targetType() {
        return typeName;
    }

    public Name targetField() {
        return field;
    }

    @Override
    public View cloneWith(Type newType, Collection<Restriction> nrs, Collection<Hint> nhs) {
        ArrayList<Restriction> rs = new ArrayList<>(nrs);
        rs.addAll(restrictions);
        HashSet<Hint> hs = new HashSet<>(nhs);
        hs.addAll(hints);
        return new View(typeName, field, auto, isConstant, constantValue, name, newType, declaredIn, rs, hs, comment);
    }

}
