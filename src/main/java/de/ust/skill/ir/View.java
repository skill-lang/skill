package de.ust.skill.ir;

/**
 * A view onto another field.
 * 
 * @see SKilL V1.0 §6.?
 * @author Timm Felden
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

    // private View(Name typeName, Name field, boolean auto, boolean isConstant, long constantValue, Name name,
    // Type newType, Declaration declaredIn, ArrayList<Restriction> rs, HashSet<Hint> hs, Comment comment) {
    // this.typeName = typeName;
    // this.field = field;
    // }

    // /**
    // * removes a !hide hint if present
    // */
    // private static Collection<Hint> unhide(Set<Hint> hints) {
    // if (hints.contains(Hint.hide)) {
    // HashSet<Hint> rval = new HashSet<>(hints);
    // rval.remove(Hint.hide);
    // return rval;
    // }
    // return hints;
    // }

    // @Override
    // public View cloneWith(Type newType, Collection<Restriction> nrs, Collection<Hint> nhs) {
    // ArrayList<Restriction> rs = new ArrayList<>(nrs);
    // rs.addAll(restrictions);
    // HashSet<Hint> hs = new HashSet<>(nhs);
    // hs.addAll(hints);
    // return new View(typeName, field, auto, isConstant, constantValue, name, newType, declaredIn, rs, hs, comment);
    // }

}
