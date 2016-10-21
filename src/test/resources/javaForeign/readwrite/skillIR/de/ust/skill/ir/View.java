package de.ust.skill.ir;

/**
 * A view onto another field.
 * 
 * @see SKilL V1.0 §4.4.4
 * @author Timm Felden
 * 
 * @note Views will not quite make it through Substitutions.
 */
public class View extends FieldLike {

	public Name ownerName;
	public Type type;
	public FieldLike target;

	public View(Name declaredIn, Type type, Name name, Comment comment) {
		super(name, comment);
		ownerName = declaredIn;
		this.target = null;
		this.type = type;
	}

	public void initialize(FieldLike target) {
		
		this.target = target;
	}

	public Name getOwnerName() {
		return ownerName;
	}

	public Type getType() {
		return type;
	}

	public FieldLike getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "view " + target.getDeclaredIn() + "." + target.getName() + "as\n" + type + " " + name;
	}
}
