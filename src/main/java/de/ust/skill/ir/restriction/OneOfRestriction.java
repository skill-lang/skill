package de.ust.skill.ir.restriction;

import java.util.Iterator;
import java.util.List;

import de.ust.skill.ir.Name;
import de.ust.skill.ir.Restriction;

public class OneOfRestriction extends Restriction {
	private final List<Name> oneOfNames;
	
	public OneOfRestriction(List<Name> oneOfNames) {
		this.oneOfNames = oneOfNames;
	}
	
	@Override
	public String getName() {
		return "oneof";
	}
	
	public List<Name> getOneOfNames() {
		return this.oneOfNames;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("@oneof(");
		Iterator<Name> it = oneOfNames.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext())
				sb.append(':');
		}
		sb.append(")");
		return sb.toString();
	}

}
