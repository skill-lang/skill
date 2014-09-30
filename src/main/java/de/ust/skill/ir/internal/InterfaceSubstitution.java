package de.ust.skill.ir.internal;

import java.util.Collections;
import java.util.List;

import de.ust.skill.ir.ContainerType;
import de.ust.skill.ir.Declaration;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.Hint;
import de.ust.skill.ir.InterfaceType;
import de.ust.skill.ir.ParseException;
import de.ust.skill.ir.Restriction;
import de.ust.skill.ir.Type;
import de.ust.skill.ir.TypeContext;

/**
 * Substitutes interfaces.
 * 
 * @author Timm Felden
 */
public class InterfaceSubstitution extends Substitution {

    public InterfaceSubstitution() {
    }

    @Override
    public void addTypes(TypeContext tc, List<Declaration> defs) throws ParseException {
        // no types are added by us
    }

    @Override
    public boolean drop(Type t) {
        return t instanceof InterfaceType;
    }

    @Override
    public Field substitute(TypeContext tc, Field f) throws ParseException {

        // just replace interface types by base types, this can be done in an
        // field agnostic way

        return f.cloneWith(substitute(tc, f.getType()), Collections.<Restriction> emptySet(),
                Collections.<Hint> emptySet());
    }

    @Override
    public Type substitute(TypeContext tc, Type t) throws ParseException {
        if (null == t)
            return null;

        // substitute interfaces with their super types
        final Type target;
        if (t instanceof InterfaceType)
            target = ((InterfaceType) t).getSuperType();
        else
            target = t;

        // convert base types to argument type context
        if (target instanceof ContainerType) {
            return ((ContainerType) target).substituteBase(tc, this);
        }
        return tc.get(target.getSkillName());
    }

}
