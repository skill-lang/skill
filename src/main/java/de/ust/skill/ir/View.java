package de.ust.skill.ir;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A view onto another field.
 * 
 * @see SKilL V1.0 §6.?
 * @author Timm Felden
 */
// TODO ensure that all Fields works correctly here:)
public class View extends Field {

    final private Field target;

    public View(Field target, Type type, Name name, String comment) throws ParseException {
        super(type, name, target.isAuto(), comment, target.restrictions, unhide(target.hints));
        this.target = target;
    }

    /**
     * removes a !hide hint if present
     */
    private static Collection<Hint> unhide(Set<Hint> hints) {
        if (hints.contains(Hint.hide)) {
            HashSet<Hint> rval = new HashSet<>(hints);
            rval.remove(Hint.hide);
            return rval;
        }
        return hints;
    }

    public Field getTarget() {
        return target;
    }

}
