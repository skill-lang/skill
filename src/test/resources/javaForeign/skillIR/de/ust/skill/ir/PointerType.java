package de.ust.skill.ir;

/**
 * Mark ground types which create reference like fields.
 * 
 * @author Timm Felden
 */
public class PointerType extends GroundType implements ReferenceType {
	PointerType(TypeContext tc, String name) {
		super(tc, name);
	}
}