package de.ust.skill.ir;

abstract public class FieldLike {

    public final Name name;
    public Declaration declaredIn;
    /**
     * The comment from Specification.
     */
    public final Comment comment;

    public FieldLike(Name name, Comment comment) {
        assert (null != name);
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

	public final Comment getComment() {
		return comment;
	}

}
