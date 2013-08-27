package de.ust.skill.ir;

public class Data extends Field{
	
	public final boolean isAuto;

	public Data(boolean isAuto, Type type, String name){
		super(type, name);
		this.isAuto = isAuto;
	}
}
