package de.ust.skill.ir;

/**
 * This exception is thrown by the front-end in case of any expected error.
 * 
 * @author Timm Felden
 */
public final class ParseException extends Exception {
	public static final long serialVersionUID = -7554775708673716216L;

    public ParseException(String msg) {
        super(msg);
    }

    public ParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
