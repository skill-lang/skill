package de.ust.skill.ir.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ust.skill.ir.Comment;
import de.ust.skill.ir.ContainerType;
import de.ust.skill.ir.Declaration;
import de.ust.skill.ir.EnumType;
import de.ust.skill.ir.Field;
import de.ust.skill.ir.Hint;
import de.ust.skill.ir.InterfaceType;
import de.ust.skill.ir.LanguageCustomization;
import de.ust.skill.ir.Name;
import de.ust.skill.ir.ParseException;
import de.ust.skill.ir.Restriction;
import de.ust.skill.ir.Type;
import de.ust.skill.ir.TypeContext;
import de.ust.skill.ir.UserType;
import de.ust.skill.ir.View;
import de.ust.skill.ir.WithFields;
import de.ust.skill.ir.restriction.SingletonRestriction;

/**
 * Substitutes enums.
 * 
 * @author Timm Felden
 */
public class EnumSubstitution extends Substitution {

	public List<EnumType> enums;
	public Map<UserType, UserType> tops = new HashMap<>();

	public EnumSubstitution(List<EnumType> enums) {
		this.enums = enums;
	}

	@Override
	public void addTypes(TypeContext tc, List<Declaration> defs) throws ParseException {
		// add abstract classes for enum names and singletons for enum instances
		for (EnumType t : enums) {
			// TODO @abstract
			UserType top = UserType.newDeclaration(tc, t.getName(), t.getComment(), Collections.<Restriction>emptySet(),
					Collections.<Hint>emptySet());
			defs.add(top);

			// enum values become singletons!
			for (Name inst : t.getInstances()) {
				// subtypes have the skill name "<enum>:<inst>"
				String skillname = t.getSkillName() + ":" + inst.getSkillName();
				Name name = new Name(Arrays.asList(skillname), skillname);

				UserType sub = UserType.newDeclaration(tc, name, Comment.NoComment.get(),
						Arrays.<Restriction>asList(new SingletonRestriction()), Collections.<Hint>emptySet());
				defs.add(sub);
				tops.put(sub, top);
			}

			// TODO @default
		}
	}

	@Override
	public boolean drop(Type t) {
		return t instanceof EnumType;
	}

	@Override
	public Field substitute(TypeContext tc, Field f) throws ParseException {

		// just replace interface types by base types, this can be done in an
		// field agnostic way

		return f.cloneWith(substitute(tc, f.getType()), Collections.<Restriction>emptySet(),
				Collections.<Hint>emptySet());
	}

	@Override
	public Type substitute(TypeContext tc, Type target) throws ParseException {
		if (null == target)
			return null;

		// @note substitution happens by magic replacement of types with equal
		// names

		// convert base types to argument type context
		if (target instanceof ContainerType) {
			return ((ContainerType) target).substituteBase(tc, this);
		}
		return tc.get(target.getSkillName());
	}

	@Override
	public void initialize(TypeContext fromTC, TypeContext tc, UserType d) throws ParseException {
		Type source = fromTC.types.get(d.getSkillName());
		// case 1: source is null, i.e. the type has been created by us (this
		// happens in case of enum instances)
		if (null == source) {
			d.initialize(tops.get(d), Collections.<InterfaceType>emptyList(), Collections.<Field>emptyList(),
					Collections.<View>emptyList(), Collections.<LanguageCustomization>emptyList());
		}
		// case 2: d always was a user type ⇒ behave as always
		else if (source instanceof UserType) {
			UserType t = (UserType) source;
			d.initialize((UserType) substitute(tc, t.getSuperType()),
					TypeContext.substituteTypes(this, tc, t.getSuperInterfaces()),
					TypeContext.substituteFields(this, tc, t.getFields()), t.getViews(), t.getCustomizations());
		}
		// case 3: we replaced an enum
		else {
			WithFields t = (WithFields) source;
			List<Field> fs = TypeContext.substituteFields(this, tc, t.getFields());
			d.initialize(null, Collections.<InterfaceType>emptyList(), fs, Collections.<View>emptyList(),
					Collections.<LanguageCustomization>emptyList());
		}
	}

}
