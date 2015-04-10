package de.ust.skill.ir.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.ust.skill.ir.ContainerType;
import de.ust.skill.ir.Declaration;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.Hint;
import de.ust.skill.ir.ParseException;
import de.ust.skill.ir.Restriction;
import de.ust.skill.ir.Type;
import de.ust.skill.ir.TypeContext;
import de.ust.skill.ir.Typedef;
import de.ust.skill.ir.View;

/**
 * Removes views.
 * 
 * @author Timm Felden
 */
public class ViewSubstitution extends Substitution {

    public ViewSubstitution() {
    }

    @Override
    public void addTypes(TypeContext tc, List<Declaration> defs) throws ParseException {
        // no types are added by us
    }

    @Override
    public boolean drop(Type t) {
        // no types are dropped
        return false;
    }

    @Override
    public Field substitute(TypeContext tc, Field f) throws ParseException {
        // drop views
        if (f instanceof View)
            return null;

        // copy actual fields
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
    public Type substitute(TypeContext tc, Type target) throws ParseException {
        if (null == target)
            return null;

        // convert base types to argument type context
        if (target instanceof ContainerType) {
            return ((ContainerType) target).substituteBase(tc, this);
        }
        return tc.get(target.getSkillName());
    }

}
