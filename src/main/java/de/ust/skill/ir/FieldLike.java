/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir;

abstract public class FieldLike {

    protected final Name name;
    protected Declaration declaredIn;
    /**
     * The comment from Specification.
     */
    private final Comment comment;

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
    protected void setDeclaredIn(Declaration declaredIn) {
        this.declaredIn = declaredIn;
    }

	public final Comment getComment() {
		return comment;
	}

}
