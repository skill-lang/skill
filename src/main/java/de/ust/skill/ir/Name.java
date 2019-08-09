package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents names of types and fields. This implementation provides conversions for various casing styles such as
 * ADA_STYLE or camelCase.
 * 
 * @note Names are immutable.
 * @author Timm Felden
 */
final public class Name implements Comparable<Name> {

    /**
     * Names with equal skillNames are equal. Thus all comparison is deferred to skillName.
     */
    final private String skillName;
    final private List<String> parts;
    final private String packagePath;
    private String internalName = "";

    public Name(List<String> parts, String skillName) {
        this(parts, skillName, "");
    }

    public Name(List<String> parts, String skillName, String packagePath) {
        this.parts = Collections.unmodifiableList(parts);
        this.skillName = skillName;
        this.packagePath = packagePath;
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
        this.packagePath = "";
    }

    public String getSkillName() {
        return skillName;
    }

    @Override
    public int compareTo(Name o) {
        return skillName.compareTo(o.skillName);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Name)
            return skillName.equals(((Name) o).skillName);
        return false;
    }

    @Override
    public int hashCode() {
        return skillName.hashCode();
    }

    @Override
    public String toString() {
        return skillName;
    }

    public String[] parts() {
        return parts.toArray(new String[parts.size()]);
    }

    // different naming conventions, alphabetical order

    private String ada;

    /**
     * @return Ada_Style
     */
    public String ada() {
        if (null == ada) {
            Iterator<String> it = parts.iterator();
            StringBuilder sb = new StringBuilder(capitalize(it.next()));
            while (it.hasNext())
                sb.append("_").append(capitalize(it.next()));
            ada = sb.toString();
        }
        return ada;
    }

    private String cStyle;

    /**
     * @return c_style, i.e. lower_case_with_under_scores
     */
    public String cStyle() {
        if (null == cStyle) {
            Iterator<String> it = parts.iterator();
            StringBuilder sb = new StringBuilder(it.next().toLowerCase());
            while (it.hasNext())
                sb.append("_").append(it.next().toLowerCase());
            cStyle = sb.toString();
        }
        return cStyle;
    }

    private String camel;

    /**
     * @return firstPartLowerCaseCamelCase
     */
    public String camel() {
        if (null == camel) {
            Iterator<String> it = parts.iterator();
            StringBuilder sb = new StringBuilder(it.next());
            while (it.hasNext())
                sb.append(capitalize(it.next()));
            camel = sb.toString();
        }
        return camel;
    }

    private String capital;

    /**
     * @return CapitalizedCamelCase
     */
    public String capital() {
        if (null == capital) {
            StringBuilder sb = new StringBuilder();
            for (String s : parts)
                sb.append(capitalize(s));
            capital = sb.toString();
        }
        return capital;
    }

    /**
     * @return skillstylealllowercase
     */
    public String lower() {
        return skillName;
    }

    /**
     * Capitalizes a string.
     */
    private static String capitalize(String arg) {
        if (Character.isUpperCase(arg.charAt(0)))
            return arg;

        return Character.toUpperCase(arg.charAt(0)) + arg.substring(1);
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String getFqdn() {
        if (getPackagePath().length() > 0)
            return getPackagePath() + "." + skillName;

        return skillName;
    }

    public void setInternalName(String name) {
        this.internalName = name.toLowerCase();
    }

    public String getInternalName() {
        if (this.internalName.length() > 0)
            return internalName;

        return skillName.toLowerCase();
    }

}
