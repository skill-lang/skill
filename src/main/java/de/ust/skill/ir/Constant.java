package de.ust.skill.ir;

public class Constant extends Field {

	public final long value;

	public Constant(Type type, String name, long val) {
		super(type, name);
		this.value = val;
	}
}
