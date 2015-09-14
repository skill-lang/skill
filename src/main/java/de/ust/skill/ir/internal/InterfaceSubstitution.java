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
import de.ust.skill.ir.UserType;

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

    @Override
    public void initialize(TypeContext fromTC, TypeContext tc, UserType d) throws ParseException {
        UserType t = (UserType) fromTC.types.get(d.getSkillName());

        // collect fields from super interfaces
        List<Field> fields = TypeContext.substituteFields(this, tc, t.getFields());
        for (InterfaceType i : t.getSuperInterfaces())
            addFieldsRecursive(tc, fields, i);

        UserType superType = findSuperType(t);

        d.initialize((UserType) substitute(tc, superType), Collections.emptyList(), fields);
    }

    private static UserType findSuperType(UserType t) {
        UserType rval = t.getSuperType();
        // search interfaces
        if (null == rval)
            for (InterfaceType i : t.getSuperInterfaces())
                if (null == rval)
                    rval = findSuperType(i);

        return rval;
    }

    private static UserType findSuperType(InterfaceType t) {
        UserType rval = null;
        if (t.getSuperType() instanceof UserType)
            rval = (UserType) t.getSuperType();

        // search interfaces
        if (null == rval)
            for (InterfaceType i : t.getSuperInterfaces())
                if (null == rval)
                    rval = findSuperType(i);

        return rval;
    }

    private void addFieldsRecursive(TypeContext tc, List<Field> fields, InterfaceType i) throws ParseException {
        fields.addAll(TypeContext.substituteFields(this, tc, i.getFields()));
        for (InterfaceType sub : i.getSuperInterfaces())
            addFieldsRecursive(tc, fields, sub);
    }

}
