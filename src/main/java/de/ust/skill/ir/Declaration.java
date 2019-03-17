package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.ust.skill.ir.restriction.AbstractRestriction;

/**
 * A declared user type.
 * 
 * @author Timm Felden
 */
public abstract class Declaration extends Type implements ReferenceType {

	// names
	protected final Name name;

	/**
	 * The restrictions applying to this declaration.
	 */
	protected final List<Restriction> restrictions;
	/**
	 * The restrictions applying to this declaration.
	 */
	protected final Set<Hint> hints;
	/**
	 * The image of the comment excluding begin( / * * ) and end( * / ) tokens.
	 */
	protected final Comment comment;

	protected Declaration(Name name, Comment comment, Collection<Restriction> restrictions, Collection<Hint> hints) {
		this.name = name;
		this.comment = comment;
		this.restrictions = new ArrayList<>(restrictions);
		this.hints = Collections.unmodifiableSet(new HashSet<Hint>(hints));
	}

	/**
	 * Declarations will depend on other declarations, thus they need to be
	 * initialized, after all declarations have been allocated.
	 * 
	 * @return true, iff initialized
	 */
	public abstract boolean isInitialized();

	/**
	 * @return pretty parsable representation of this type
	 */
	public abstract String prettyPrint();

	@Override
	final public String getSkillName() {
		return name.getSkillName();
	}

	@Override
	final public Name getName() {
		return name;
	}

	/**
	 * The image of the comment excluding begin( / * * ) and end( * / ) tokens.
	 * This may require further transformation depending on the target language.
	 * 
	 * @note can contain newline characters!!!
	 */
	public Comment getComment() {
		return comment;
	}

	public List<Restriction> getRestrictions() {
		return restrictions;
	}

	public boolean isAbstract() {
		for (Restriction restriction : restrictions) {
			if (restriction instanceof AbstractRestriction) {
				return true;
			}
		}
		return false;
	}

	public boolean isUnique() {
		return hints.contains(Hint.unique);
	}

	public boolean isPure() {
		return hints.contains(Hint.pure);
	}

	public abstract boolean isMonotone();

	public abstract boolean isReadOnly();

	public boolean isIgnored() {
		return hints.contains(Hint.ignore);
	}

	public Collection<Hint> getHints() {
		return hints;
	}

	/**
	 * Search hints for a pragma of argument name. Return null, if not present.
	 * 
	 * @param id
	 *            name of the pragma
	 * @return list of arguments, if present, null else
	 */
	public List<Name> getPragma(String id) {
		Optional<Hint> hint = hints.stream().filter(
				h -> Hint.Type.pragma == h.type() && h.arguments().get(0).getSkillName().equals(id.toLowerCase()))
				.findFirst();
		if (hint.isPresent()) {
			List<Name> args = hint.get().arguments();
			return args.subList(1, args.size());
		}
		return null;
	}

	/**
	 * create a copy of this in tc
	 * 
	 * @param tc
	 *            an argument type context that is different from the type
	 *            context this is living in
	 * @return an equivalent copy uninitialized copy of this
	 */
	abstract Declaration copy(TypeContext tc);
}
