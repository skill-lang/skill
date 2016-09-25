package de.ust.skill.ir;

import java.util.List;
import java.util.Map;

/**
 * An auto field like property of a language that will be mixed into the code by a target language generator.
 * 
 * @author Timm Felden
 */
public class LanguageCustomization extends FieldLike {

    /**
     * the name of the target language generator
     */
    public String language;

    /**
     * options provided to the field using '!'.
     */
    //public Map<String, List<String>> options;

    /**
     * the type is provided in form of a string, in order to type the field
     */
    public String type;

    public LanguageCustomization(Name name, Comment comment, Name language, String type) {
        super(name, comment);
        this.language = language.lower();
        this.type = type;
    }

}
