/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

/**
 * Top level restriction type.
 *
 * @author Timm Felden
 */
sealed abstract class Restriction(val ID: Long);

/**
 * This type is used to tag restrictions with miss-match recovery strategies.
 */
sealed trait RecoverableRestriction;

/**
 * Those can be ignored on read and added on write.
 */
sealed abstract class IgnorableRestriction(ID: Long) extends Restriction(ID) with RecoverableRestriction;

final case object Nullable extends IgnorableRestriction(0);
final case object Unique extends IgnorableRestriction(2);
final case object Singleton extends IgnorableRestriction(4);
final case object Monotone extends IgnorableRestriction(6);

final case class Range[@specialized T](min: T, max: T) extends Restriction(1);
final case object ConstantLengthPointer extends Restriction(3) with RecoverableRestriction;
final case class Coding(name: String) extends Restriction(5);
