package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents names of types and fields. This implementation provides
 * conversions for various casing styles such as ADA_STYLE or camelCase.
 * 
 * @note Names are immutable.
 * @author Timm Felden
 */
final public class Name implements Comparable<Name> {

    final private String skillName;
    final private List<String> parts;

    public Name(List<String> parts, String skillName) {
        this.parts = Collections.unmodifiableList(parts);
        this.skillName = skillName;
    }

    /**
     * Constructor for built-in names
     * 
     * @param skillName
     */
    Name(String skillName) {
        List<String> parts = new ArrayList<>(1);
        parts.add(skillName);
        this.parts = Collections.unmodifiableList(parts);
        this.skillName = skillName;
    }

    public String getSkillName() {
        return skillName;
    }

    @Override
    public int compareTo(Name o) {
        return skillName.compareTo(o.skillName);
    }

    // different naming conventions, alphabetical order

    private String ada;

    public String ada() {
        if (null == ada) {
            throw new Error();
        }
        return ada;
    }

    private String camel;

    public String camel() {
        if (null == camel) {
            throw new Error();
        }
        return camel;
    }

    private String capital;

    public String capital() {
        if (null == capital) {
            throw new Error();
        }
        return capital;
    }

    public String lower() {
        return skillName;
    }
}
