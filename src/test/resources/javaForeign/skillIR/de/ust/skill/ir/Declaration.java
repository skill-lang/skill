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
public class Declaration extends Type implements ReferenceType {

	// names
	public Name name;

	/**
	 * The restrictions applying to this declaration.
	 */
	public List<Restriction> restrictions;
	/**
	 * The restrictions applying to this declaration.
	 */
	public Set<Hint> hints;
	/**
	 * The image of the comment excluding begin( / * * ) and end( * / ) tokens.
	 */
	public Comment comment;

	public Declaration(Name name, Comment comment, Collection<Restriction> restrictions, Collection<Hint> hints) {
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
	public boolean isInitialized() {return false;}

	/**
	 * @return pretty parsable representation of this type
	 */
	public String prettyPrint() {return null;}

	@Override
	public String getSkillName() {
		return name.getSkillName();
	}

	@Override
	public Name getName() {
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

	public boolean isMonotone() {return false;}

	public boolean isReadOnly() {return false;}

	public boolean isIgnored() {
		return hints.contains(Hint.ignore);
	}

	public Set<Hint> getHints() {
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
				h -> HintType.pragma == h.type() && h.arguments().get(0).getSkillName().equals(id.toLowerCase()))
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
	Declaration copy(TypeContext tc) {return null;}
}
