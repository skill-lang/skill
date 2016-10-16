package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of a regular user type.
 * 
 * @author Timm Felden
 */
final public class UserType extends Declaration implements WithFields {

	/**
	 * super type is the type above this type. base type is the base type of the
	 * formed type tree. This can even be <i>this</i>.
	 */
	private UserType superType = null, baseType = null;
	private final List<UserType> children = new ArrayList<>();

	// fields
	private List<Field> fields = null;
	private List<InterfaceType> interfaces;
	private List<View> views;
	private List<LanguageCustomization> customizations;

	/**
	 * Creates a declaration of type name.
	 * 
	 * @throws ParseException
	 *             thrown, if the declaration to be constructed is in fact
	 *             illegal
	 * @note the declaration has to be completed, i.e. it has to be evaluated in
	 *       pre-order over the type hierarchy.
	 */
	private UserType(Name name, Comment comment, Collection<Restriction> restrictions, Collection<Hint> hints)
			throws ParseException {
		super(name, comment, restrictions, hints);

		superType = baseType = null;
	}

	@Override
	UserType copy(TypeContext tc) {
		try {
			return newDeclaration(tc, name, comment, restrictions, hints);
		} catch (ParseException e) {
			throw new Error("cannot happen", e);
		}
	}

	/**
	 * @param name
	 * @return a new declaration which is registered at types.
	 * @throws ParseException
	 *             if the declaration is already present
	 */
	public static UserType newDeclaration(TypeContext tc, Name name, Comment comment,
			Collection<Restriction> restrictions, Collection<Hint> hints) throws ParseException {
        String skillName = name.getFqdn();
		if (tc.types.containsKey(skillName))
			throw new ParseException("Duplicate declaration of type " + name);

		UserType rval = new UserType(name, comment, restrictions, hints);
		tc.types.put(skillName, rval);
		return rval;
	}

	@Override
	public boolean isInitialized() {
		return null != baseType;
	}

	/**
	 * Initializes the type declaration with data obtained from parsing the
	 * declarations body.
	 * 
	 * @param SuperType
	 * @param Fields
	 * @param interfaces
	 * @throws ParseException
	 *             thrown if the declaration is illegal, e.g. because it
	 *             contains illegal hints
	 */
	public void initialize(UserType SuperType, List<InterfaceType> interfaces, List<Field> Fields, List<View> views,
			List<LanguageCustomization> customizations) throws ParseException {
		assert !isInitialized() : "multiple initialization";
		assert null != Fields : "no fields supplied";
		// check for duplicate fields
		{
			Set<Name> names = new HashSet<>();
            for (FieldLike f : Fields) {
                names.add(f.name);
                f.setDeclaredIn(this);
            }
            for (FieldLike f : views) {
                f.setDeclaredIn(this);
            }
			if (names.size() != Fields.size())
				throw new ParseException("Type " + name + " contains duplicate field definitions.");
		}

		if (null != SuperType) {
			assert null != SuperType.baseType : "types have to be initialized in pre-order";

			this.superType = SuperType;
			this.baseType = SuperType.baseType;
			SuperType.children.add(this);
		} else {
			baseType = this;
		}
		this.interfaces = interfaces;

		this.fields = Fields;
		this.views = views;
		this.customizations = customizations;

		// check hints
		Hint.checkDeclaration(this, this.hints);
	}

	public UserType getBaseType() {
		return baseType;
	}

	public UserType getSuperType() {
		return superType;
	}

	public List<InterfaceType> getSuperInterfaces() {
		return interfaces;
	}

	/**
	 * @return a list of super interfaces and the super type, if exists
	 */
	public List<Declaration> getAllSuperTypes() {
		ArrayList<Declaration> rval = new ArrayList<Declaration>();
		rval.addAll(interfaces);
		if (null != superType)
			rval.add(superType);

		return rval;
	}

	/**
	 * @return a list of types, that use this type as direct super type
	 */
	public List<UserType> getSubTypes() {
		return children;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.ust.skill.ir.WithFields#getFields()
	 */
	@Override
	public List<Field> getFields() {
		assert isInitialized() : this.name + " has not been initialized";
		return fields;
	}

	/**
	 * @return all fields of an instance of the type, including fields declared
	 *         in super types
	 */
	public List<Field> getAllFields() {
		assert isInitialized() : "you can not obtain fields of type " + name + " because it is not initialized";
		List<Field> rval = new ArrayList<>(fields);
		if (null != superType)
			rval.addAll(superType.getAllFields());

		for (InterfaceType i : interfaces) {
			List<Field> tmp = i.getAllFields();
			tmp.removeAll(rval);
			rval.addAll(tmp);
		}

		return rval;
	}

	/**
	 * @return pretty parsable representation of this type
	 */
	@Override
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder(name.getSkillName());
		if (null != superType) {
			sb.append(":").append(superType.name);
		}
		sb.append(" {\n");
		for (FieldLike f : fields)
			sb.append("\t").append(f.toString()).append(";\n");
		sb.append("}\n");

		return sb.toString();
	}

	@Override
	public boolean isMonotone() {
		if (this == baseType)
			return hints.contains(Hint.monotone) || hints.contains(Hint.readonly);
		return baseType.isMonotone();
	}

	@Override
	public boolean isReadOnly() {
		if (this == baseType)
			return hints.contains(Hint.readonly);
		return baseType.isReadOnly();
	}

	@Override
	public List<View> getViews() {
		return views;
	}

	@Override
	public List<LanguageCustomization> getCustomizations() {
		return customizations;
	}
}
