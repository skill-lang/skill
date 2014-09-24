package de.ust.skill.ir.internal;

import de.ust.skill.ir.ContainerType;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.ParseException;
import de.ust.skill.ir.Type;
import de.ust.skill.ir.TypeContext;
import de.ust.skill.ir.Typedef;

/**
 * Substitutes typedefs.
 * 
 * @author Timm Felden
 */
public class TypedefSubstitution extends Substitution {

    public TypedefSubstitution() {
    }

    @Override
    public boolean drop(Type t) {
        return t instanceof Typedef;
    }

    @Override
    public Field substitute(TypeContext tc, Field f) {


        // filter types in fields by replacing a typedef with its definition
        // adding descriptions to the field, except for comment (the comment
        // describes the type, not the field!)

        return null;
    }

    @Override
    public Type substitute(TypeContext tc, Type t) throws ParseException {
        if (t instanceof ContainerType)
            throw new Error("TODO container!!!");

        return tc.get((t instanceof Typedef ? ((Typedef) t).getTarget() : t).getSkillName());
    }

}
