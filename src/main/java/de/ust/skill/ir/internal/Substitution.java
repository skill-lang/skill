package de.ust.skill.ir.internal;

import de.ust.skill.ir.Field;
import de.ust.skill.ir.ParseException;
import de.ust.skill.ir.Type;
import de.ust.skill.ir.TypeContext;

/**
 * Type substitution that can be used to modify a type context. The substitution
 * provides a predicate drop, that decides whether or not to drop a type in the
 * substitution and a type replacement predicate that will be used in the
 * process of cloning a state to replace types by other types.
 * 
 * @author Timm Felden
 */
abstract public class Substitution {

    /**
     * Substitution is done on fields, because some type
     * information(restrictions/hints) is part of the field and not of the type
     * itself. Furthermore, fields with a dropped type will not be dropped!
     * 
     * @note the substitution will copy the type of the argument field by using
     *       types available in the argument type context
     */
    public abstract Field substitute(TypeContext tc, Field f) throws ParseException;

    /**
     * Substitution of super and target types.
     */
    public abstract Type substitute(TypeContext tc, Type t) throws ParseException;

    public abstract boolean drop(Type t);
}
