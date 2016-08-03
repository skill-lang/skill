package de.ust.skill.ir;

/**
 * A view onto another field.
 * 
 * @see SKilL V1.0 §4.4.4
 * @author Timm Felden
 * 
 * @note Views will not quite make it through Substitutions.
 */
final public class View extends FieldLike {

	final private Name ownerName;
	final private Type type;
	final private FieldLike target;

	public View(Name declaredIn, FieldLike target, Type type, Name name, Comment comment) {
		super(name, comment);
		ownerName = declaredIn;
		this.target = target;
		this.type = type;
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
}
