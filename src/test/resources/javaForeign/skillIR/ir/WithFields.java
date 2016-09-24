package de.ust.skill.ir;

import java.util.List;

public interface WithFields {

	/**
	 * @return the fields added in this type
	 */
	public abstract List<Field> getFields();

	/**
	 * @return views of this entity
	 */
	public abstract List<View> getViews();

	/**
	 * @return language customizations of this entity
	 */
	public abstract List<LanguageCustomization> getCustomizations();
}
