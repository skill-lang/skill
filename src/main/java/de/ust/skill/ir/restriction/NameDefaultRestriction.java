package de.ust.skill.ir.restriction;

import java.util.List;

import de.ust.skill.parser.Name;

/**
 * @author Dennis Przytarski
 */
final public class NameDefaultRestriction extends DefaultRestriction {

	private final List<Name> value;

	public NameDefaultRestriction(List<Name> value) {
		this.value = value;
	}

	public List<Name> getValue() {
		return this.value;
	}

}
