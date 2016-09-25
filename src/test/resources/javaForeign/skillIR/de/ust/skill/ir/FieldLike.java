package de.ust.skill.ir;

abstract public class FieldLike {

    public Name name;
    public Declaration declaredIn;
    /**
     * The comment from Specification.
     */
    public Comment comment;

    public FieldLike(Name name, Comment comment) {
        
        this.name = name;
        this.comment = comment;
    }

    public Name getName() {
        return name;
    }

    public String getSkillName() {
        return name.lower();
    }

    /**
     * @return the enclosing declaration.
     */
    public Declaration getDeclaredIn() {
        return declaredIn;
    }

    /**
     * Invoked during construction of the enclosing declaration.
     */
    public void setDeclaredIn(Declaration declaredIn) {
        this.declaredIn = declaredIn;
    }

	public Comment getComment() {
		return comment;
	}

}
