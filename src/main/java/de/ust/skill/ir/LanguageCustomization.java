package de.ust.skill.ir;

import java.util.List;
import java.util.Map;

/**
 * An auto field like property of a language that will be mixed into the code by a target language generator.
 * 
 * @author Timm Felden
 */
final public class LanguageCustomization extends FieldLike {

    /**
     * the name of the target language generator
     */
    final Name language;

    /**
     * options provided to the field using '!'.
     */
    final Map<Name, List<String>> options;

    /**
     * the type is provided in form of a string, in order to type the field
     */
    final String type;

    public LanguageCustomization(Name name, Comment comment, Name language, String type,
            Map<Name, List<String>> options) {
        super(name, comment);
        this.language = language;
        this.options = options;
        this.type = type;
    }
}
