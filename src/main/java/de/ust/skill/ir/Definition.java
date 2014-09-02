package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of a regular user type.
 * 
 * @author Timm Felden
 */
final public class Definition extends Declaration {

	/**
	 * super type is the type above this type. base type is the base type of the
	 * formed type tree. This can even be <i>this</i>.
	 */
	private Definition superType = null, baseType = null;
	private final List<Definition> children = new ArrayList<>();

	// fields
	private List<Field> fields = null;

	/**
	 * Creates a declaration of type name.
	 * 
	 * @throws ParseException
	 *             thrown, if the declaration to be constructed is in fact
	 *             illegal
	 * 
	 * @note the declaration has to be completed, i.e. it has to be evaluated in
	 *       pre-order over the type hierarchy.
	 */
    private Definition(Name name, String comment, List<Restriction> restrictions, List<Hint> hints)
			throws ParseException {
        super(name, comment, restrictions, hints);

		superType = baseType = null;
	}

	/**
	 * @param name
	 * @return a new declaration which is registered at types.
	 * @throws ParseException
	 *             if the declaration is already present
	 */
    public static Definition newDeclaration(TypeContext tc, Name name, String comment,
			List<Restriction> restrictions, List<Hint> hints) throws ParseException {
        String skillName = name.getSkillName();
		if (tc.types.containsKey(skillName))
			throw new ParseException("Duplicate declaration of type " + name);

		Definition rval = new Definition(name, comment, restrictions, hints);
		tc.types.put(skillName, rval);
		return rval;
	}

	public boolean isInitialized() {
		return null != baseType;
	}

	/**
	 * Initializes the type declaration with data obtained from parsing the
	 * declarations body.
	 * 
	 * @param SuperType
	 * @param Fields
	 * @throws ParseException
	 *             thrown if the declaration is illegal, e.g. because it
	 *             contains illegal hints
	 */
	public void initialize(Definition SuperType, List<Field> Fields) throws ParseException {
		assert !isInitialized() : "multiple initialization";
		assert null != Fields : "no fields supplied";
		// check for duplicate fields
		{
			Set<String> names = new HashSet<>();
			for (Field f : Fields)
				names.add(f.name);
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

		this.fields = Fields;

		// check hints
		Hint.checkDeclaration(this, this.hints);
	}

	public Definition getBaseType() {
		return baseType;
	}

	public Definition getSuperType() {
		return superType;
	}

	/**
	 * @return the fields added in this type
	 */
	public List<Field> getFields() {
		assert isInitialized() : this.name + " has not been initialized";
		return fields;
	}

	/**
	 * @return all fields of an instance of the type, including fields declared
	 *         in super types
	 */
	public List<Field> getAllFields() {
		if (null != superType) {
			List<Field> f = superType.getAllFields();
			f.addAll(fields);
			return f;
		}
		return new ArrayList<>(fields);
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
		sb.append("{");
		for (Field f : fields)
			sb.append("\t").append(f.toString()).append("\n");
		sb.append("}");

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
}
