package de.ust.skill.ir;

import java.util.List;

/**
 * A tag with text. See language specification for details.
 * 
 * @author Timm Felden
 */
public class Tag {
    public String name;
    public List<String> text;

    public Tag(String name) {
        this.name = name;

    }
}