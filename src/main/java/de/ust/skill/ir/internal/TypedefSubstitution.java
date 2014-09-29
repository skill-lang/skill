package de.ust.skill.ir.internal;

import java.util.Collection;
import java.util.Collections;

import de.ust.skill.ir.ContainerType;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.Hint;
import de.ust.skill.ir.ParseException;
import de.ust.skill.ir.Restriction;
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
    public Field substitute(TypeContext tc, Field f) throws ParseException {

        // filter types in fields by replacing a typedef with its definition
        // adding descriptions to the field, except for comment (the comment
        // describes the type, not the field!)

        Collection<Restriction> rs;
        Collection<Hint> hs;
        Type t = f.getType();
        if (t instanceof Typedef) {
            rs = ((Typedef) t).getRestrictions();
            hs = ((Typedef) t).getHints();
        } else {
            rs = Collections.emptySet();
            hs = Collections.emptySet();
        }
        return f.cloneWith(substitute(tc, f.getType()), rs, hs);
    }

    @Override
    public Type substitute(TypeContext tc, Type t) throws ParseException {
        if (null == t)
            return null;

        if (t instanceof ContainerType){
            return ((ContainerType) t).substituteBase(tc, this);
        }

        return tc.get((t instanceof Typedef ? ((Typedef) t).getTarget() : t).getSkillName());
    }

}
