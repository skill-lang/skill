package de.ust.skill.ir;

import java.util.List;

public interface WithFields {

    /**
     * @return the fields added in this type
     */
    public abstract List<Field> getFields();

}
