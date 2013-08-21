package de.ust.skill.parser

/**
 * Transforms a list of definition nodes back into an equivalent skill file, which can be parsed again. The result is a
 *  single file though.
 */
object ASTPrettyPrinter {
  def prettyPrint(nodes: List[Node]) = nodes.map(n => printNode(n)).foldLeft("")(_ + "\n\n" + _);

  def printNode(n: Node): String = n match {
    case n: Restriction => "@" + n.name + "(" + printList(n.args) + ")" + " "
    case n: Hint => "!" + n.name + " "
    case n: Description => (n.restrictions.map(printNode) ++ n.hints.map(printNode))
      .fold(n.comment.getOrElse(""))(_ + "\n" + _)

    case n: MapType => "map<" + printList(n.args) + ">"
    case n: SetType => "set<" + n.baseType + ">"
    case n: ListType => "list<" + n.baseType + ">"
    case n: ConstantArrayType => n.baseType + "[" + n.length + "]"
    case n: DependentArrayType => n.baseType + "[" + n.lengthFieldName + "]"
    case n: ArrayType => n.baseType + "[]"
    case n: BaseType => n.name

    case n: Constant => (printNode(n.description) + "\nconst " + printNode(n.t) + " " + n.name
      + " = 0x" + n.value.toHexString + "\n")
    case n: Data => (printNode(n.description) + (if (n.isAuto) "\nauto " else "\n") + printNode(n.t) + " "
      + n.name + "\n")

    case n: Definition => printNode(n.description) +
      n.name +
      (if (n.parent.isDefined) " : " + n.parent.get else"") + n.body.map(printNode).fold(" {\n")(_ + _) + "}\n" 
  }

  private def printList(s: List[Any]): String = {
    s match {
      case x :: xs => return xs.map(x => x.toString).fold(x.toString)(_ + ", " + _);
      case _ => return ""
    }
  }
}