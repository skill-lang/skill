package de.ust.skill.ir;

/**
 * Null object of comments, i.e. not a comment. Makes code generation a lot
 * easier. And resulting code visually appealing.
 * 
 * @author Timm Felden
 */
public class NoComment extends Comment {

    public static NoComment instance = new NoComment();

    public static NoComment get() {
        return instance;
    }

    @Override
    public String format(String prefix, String linePrefix, int lineWidth, String postfix) {
        return "";
    }

    @Override
    public String toString() {
        return "";
    }
}