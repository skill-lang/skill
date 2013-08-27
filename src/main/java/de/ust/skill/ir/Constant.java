package de.ust.skill.ir;

public class Constant extends Field{
	
	public final long val;

	public Constant(Type type, String name, long val){
		super(type, name);
		this.val = val;
	}
}
