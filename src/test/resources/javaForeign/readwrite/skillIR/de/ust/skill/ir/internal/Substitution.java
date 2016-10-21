package de.ust.skill.ir.internal;

import java.util.List;

import de.ust.skill.ir.Declaration;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.ParseException;
import de.ust.skill.ir.Type;
import de.ust.skill.ir.TypeContext;
import de.ust.skill.ir.UserType;

/**
 * Type substitution that can be used to modify a type context. The substitution
 * provides a predicate drop, that decides whether or not to drop a type in the
 * substitution and a type replacement predicate that will be used in the
 * process of cloning a state to replace types by other types.
 * 
 * @author Timm Felden
 */
public class Substitution {

	/**
	 * Substitution is done on fields, because some type
	 * information(restrictions/hints) is part of the field and not of the type
	 * itself. Furthermore, fields with a dropped type will not be dropped!
	 * 
	 * @note the substitution will copy the type of the argument field by using
	 *       types available in the argument type context
	 */
	public Field substitute(TypeContext tc, Field f) throws ParseException {return null;}

	/**
	 * Substitution of super and target types.
	 */
	public Type substitute(TypeContext tc, Type t) throws ParseException {return null;}

	/**
	 * decides to drop a type from the type context
	 */
	public boolean drop(Type t) {return false;}

	/**
	 * hook used to add new types before initialization of types
	 */
	public void addTypes(TypeContext tc, List<Declaration> defs) throws ParseException {}

	/**
	 * initialize a user type inside of tc
	 */
	public void initialize(TypeContext fromTC, TypeContext tc, UserType d) throws ParseException {
		UserType t = (UserType) fromTC.types.get(d.getSkillName());
		d.initialize((UserType) substitute(tc, t.getSuperType()),
				TypeContext.substituteTypes(this, tc, t.getSuperInterfaces()),
				TypeContext.substituteFields(this, tc, t.getFields()), t.getViews(), t.getCustomizations());
	}
}
