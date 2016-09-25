package de.ust.skill.ir.restriction;

import java.util.Iterator;
import java.util.List;

import de.ust.skill.ir.Name;

/**
 * @author Dennis Przytarski
 */
public class NameDefaultRestriction extends DefaultRestriction {

	public List<Name> value;

	public NameDefaultRestriction(List<Name> value) {
		this.value = value;
	}

	public List<Name> getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("@default(");
		Iterator<Name> vs = value.iterator();
		while (vs.hasNext()) {
			sb.append(vs.next());
			if (vs.hasNext())
				sb.append(':');
		}
		sb.append(")");
		return sb.toString();
	}
}
