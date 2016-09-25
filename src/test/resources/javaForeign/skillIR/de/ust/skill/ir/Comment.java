package de.ust.skill.ir;

import java.util.LinkedList;
import java.util.List;

/**
 * A SKilL comment providing abstraction and formatting of documentation
 * provided by the specification.
 * 
 * @author Timm Felden
 */
public class Comment {

    public List<String> text = null;
    public LinkedList<Tag> tags = new LinkedList<Tag>();

    public Comment() {
    }

    /**
     * called by the parser
     */
    public void init(List<String> text) {
        if (null == this.text)
            this.text = text;
        else
            tags.getLast().text = text;
    }

    /**
     * called by the parser
     */
    public void init(List<String> text, String tag) {
        if (null == this.text)
            this.text = text;
        else
            tags.getLast().text = text;
        tags.addLast(new Tag(tag));
    }

    /**
     * Creates a nicely formatted String with line breaks and a prefix for a
     * code generators output.
     * 
     * @note examples use ° instead of *
     * @param prefix
     *            Prefix of the comment, e.g. "  /°°"
     * @param linePrefix
     *            Prefix of a line, e.g. "   ° "
     * @param lineWidth
     *            Maximum characters in a line, e.g. 80 or 120
     * @param postfix
     *            Postfix of a comment, e.g. "   °/"
     * @return a nicely formatted string, very similar to scala's mkString,
     *         except that it tries to fill lines
     */
    public String format(String prefix, String linePrefix, int lineWidth, String postfix) {
        StringBuilder sb = new StringBuilder(prefix);

        formatText(text, linePrefix, lineWidth, sb, null);
        for (Tag t : tags)
            formatText(t.text, linePrefix, lineWidth, sb, t.name);

        // finish comment
        sb.append(postfix);
        return sb.toString();
    }

    /**
     * format a list of words
     */
    public static void formatText(List<String> text, String linePrefix, int lineWidth, StringBuilder sb, String tag) {
        StringBuilder line = new StringBuilder(linePrefix);

        if (null != tag)
            line.append(" @").append(tag).append(' ');

        for (String w : text) {
            if (line.length() + w.length() + 1 > lineWidth) {
                // break line
                sb.append(line).append('\n');
                line = new StringBuilder(linePrefix);
            }
            line.append(' ').append(w);
        }
        sb.append(line).append('\n');
    }

    @Override
    public String toString() {
        return text.toString();
    }
}
