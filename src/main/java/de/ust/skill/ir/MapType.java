/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.List;

import de.ust.skill.ir.internal.Substitution;

/**
 * @author Timm Felden
 */
public class MapType extends ContainerType {
    private final List<Type> baseTypes;

    public static Type make(TypeContext tc, List<Type> baseTypes) {
        return tc.unifyType(new MapType(baseTypes));
    }

    private MapType(List<Type> baseTypes) {
        this.baseTypes = baseTypes;
    }

    public List<Type> getBaseTypes() {
        return baseTypes;
    }

    @Override
    public String getSkillName() {
        StringBuilder sb = new StringBuilder("map<");
        boolean comma = false;
        for (Type t : baseTypes) {
            if (comma)
                sb.append(",");
            sb.append(t.getSkillName());
            comma = true;
        }
        sb.append(">");
        return sb.toString();
    }

    @Override
    public Type substituteBase(TypeContext tc, Substitution substitution) throws ParseException {
        ArrayList<Type> bs = new ArrayList<>(baseTypes.size());
        for (Type t : baseTypes) {
            Type sub = substitution.substitute(tc, t);
            if (sub instanceof ContainerType)
                throw new ParseException("Can not substitute a containertype into a map: " + sub);
            bs.add(sub);
        }
        return make(tc, bs);
    }
}
