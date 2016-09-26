package de.ust.skill.ir;

import java.util.List;

public interface WithFields {

	/**
	 * @return the fields added in this type
	 */
	public List<Field> getFields();

	/**
	 * @return views of this entity
	 */
	public List<View> getViews();

	/**
	 * @return language customizations of this entity
	 */
	public List<LanguageCustomization> getCustomizations();
}
