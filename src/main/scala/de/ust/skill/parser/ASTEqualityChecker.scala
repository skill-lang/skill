package de.ust.skill.parser

object ASTEqualityChecker {
  def check(n: Node, o: Any): Boolean = {
    // ensure type equality first
    if (n.getClass() != o.getClass) {
      false;
    } else {
      // perform a deep comparison
      n match {
        case n: Restriction ⇒ n.name.equals(o.asInstanceOf[Restriction].name) &&
          checkOrderedList(n.args, o.asInstanceOf[Restriction].args)
        case n: Hint ⇒ n.name.equals(o.asInstanceOf[Hint].name)

        case n: Description ⇒ n.comment.equals(o.asInstanceOf[Description].comment) &&
          checkOrderedList(n.hints, o.asInstanceOf[Description].hints) &&
          checkOrderedList(n.restrictions, o.asInstanceOf[Description].restrictions)

        case n: MapType  ⇒ checkOrderedList(n.args, o.asInstanceOf[MapType].args)
        case n: SetType  ⇒ n.baseType.equals(o.asInstanceOf[SetType].baseType)
        case n: ListType ⇒ n.baseType.equals(o.asInstanceOf[ListType].baseType)
        case n: ConstantArrayType ⇒ n.baseType.equals(o.asInstanceOf[ConstantArrayType].baseType) &&
          n.length == o.asInstanceOf[ConstantArrayType].length
        case n: ArrayType ⇒ n.baseType.equals(o.asInstanceOf[ArrayType].baseType)
        case n: BaseType  ⇒ n.name.equals(o.asInstanceOf[BaseType].name)

        case n: Constant ⇒ n.t.equals(o.asInstanceOf[Constant].t) &&
          n.description.equals(o.asInstanceOf[Constant].description) &&
          n.name.equals(o.asInstanceOf[Constant].name) &&
          n.value == o.asInstanceOf[Constant].value

        case n: Data ⇒ n.t.equals(o.asInstanceOf[Data].t) &&
          n.description.equals(o.asInstanceOf[Data].description) &&
          n.name.equals(o.asInstanceOf[Data].name) &&
          n.isAuto == o.asInstanceOf[Data].isAuto

        case n: Definition ⇒ n.parent.equals(o.asInstanceOf[Definition].parent) &&
          n.description.equals(o.asInstanceOf[Definition].description) &&
          n.name.equals(o.asInstanceOf[Definition].name) &&
          checkFieldList(n.body, o.asInstanceOf[Definition].body)
      }
    }
  }

  def checkDefinitionList(a: List[Definition], b: List[Definition]): Boolean = if (a.size != b.size) { false } else {
    val lt = ((a: Definition, b: Definition) ⇒ a.name.compareTo(b.name) < 0)

    val l = a.sortWith(lt)
    val r = b.sortWith(lt)

    checkOrderedList(l, r)
  }

  private def checkFieldList(a: List[Field], b: List[Field]): Boolean = {
    if (a.size != b.size)
      return false

    val lt = ((a: Field, b: Field) ⇒ (a, b) match {
      case (a: Data, b: Data)         ⇒ a.name.compareTo(b.name) < 0
      case (a: Constant, b: Constant) ⇒ a.name.compareTo(b.name) < 0
      case _                          ⇒ false
    })

    val l = a.sortWith(lt)
    val r = b.sortWith(lt)

    return checkOrderedList(l, r)
  }

  /**
   * assume list do not contain nulls
   */
  private def checkOrderedList(a: List[Any], b: List[Any]): Boolean = {
    if (a.size != b.size)
      return false

    for (i ← 0 until a.size)
      if (!a(i).equals(b(i)))
        return false

    return true
  }
}
