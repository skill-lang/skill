package de.ust.skill.ir;

import java.util.List;

public interface WithInheritance extends WithFields {

    Type getBaseType();

    Type getSuperType();

    List<InterfaceType> getSuperInterfaces();

    /**
     * @return a list of super interfaces and the super type, if exists
     */
    List<Declaration> getAllSuperTypes();
}