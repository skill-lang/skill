/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

import de.ust.skill.generator.common.Generator
import java.util.Date

/**
 * Use this to create a 5 lines header that looks similar in all languages.
 *
 * This corresponds to the -hN, -u, -license, -date options.
 *
 * @author Timm Felden
 */
case class HeaderInfo(
    protected[main] var line1 : Option[String] = None,
    protected[main] var line2 : Option[String] = None,
    protected[main] var line3 : Option[String] = None,
    protected[main] var license : Option[String] = None,
    protected[main] var userName : Option[String] = None,
    protected[main] var date : Option[String] = None) {

  private val logo = """  ___ _  ___ _ _      
 / __| |/ (_) | |     
 \__ \ ' <| | | |__   
 |___/_|\_\_|_|____|  
                      """.split('\n')

  /**
   * create a header out of language specific format specifications
   */
  def format(gen : Generator,
             topLeft : String,
             topRight : String,
             left : String,
             right : String,
             bottomLeft : String,
             bottomRight : String) : String = {

    // create header lines
    val headerLine1 = (line1 match {
      case Some(s) ⇒ s
      case None    ⇒ license.map("LICENSE: " + _).getOrElse(s"Your SKilL ${gen.getLanguageName} Binding")
    })
    val headerLine2 = (line2 match {
      case Some(s) ⇒ s
      case None ⇒ "generated: " + (date match {
        case Some(s) ⇒ s
        case None    ⇒ (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
      })
    })
    val headerLine3 = (line3 match {
      case Some(s) ⇒ s
      case None ⇒ "by: " + (userName match {
        case Some(s) ⇒ s
        case None    ⇒ System.getProperty("user.name")
      })
    })

    // create content
    val content = Array[String](
      topLeft + logo(0),
      left + logo(1) + headerLine1,
      left + logo(2) + headerLine2,
      left + logo(3) + headerLine3,
      bottomLeft + logo(4)
    )

    // create right bar, so that we can trim
    val rightSide = Array[String](topRight, right, right, right, bottomRight)

    (for ((l, r) ← content.zip(rightSide)) yield {
      val length = gen.lineLength - (if (-1 != r.indexOf('\n')) r.indexOf('\n') else r.length)

      l.padTo(length, " ").mkString.substring(0, length) + r
    }).mkString("", "\n", "\n")
  }
}