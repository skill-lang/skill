/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

import java.io.File

import de.ust.skill.BuildInfo
import de.ust.skill.common.scala.api.ThrowException
import de.ust.skill.parser.Parser

trait IROptions extends AbstractOptions {
  case class IRConfig(
      target : String,
      header : HeaderInfo = new HeaderInfo(),
      listTools : Boolean = false,
      toolName : String = null,
      newTarget : String = null,
      dropTargets : Boolean = false,
      toolDescription : String = null,
      selectTypes : Array[String] = null,
      addTool : Boolean = false,
      build : Boolean = false,
      fromSpec : File = null) extends WithProcess {

    def process {
      // create ir from spec, if requested, otherwise try to load it from target
      val sir = if (null != fromSpec) {
        val tc = Parser.process(fromSpec)
        new SIRHandler(tc, target)
      } else {
        new SIRHandler(target)
      }

      if (listTools) {
        println("known tools are:")
        for (t ← sir.sf.Tool)
          println(s"${t.name}\n  ${t.description}")
      }

      // create or select tool
      if (null != toolName) {
        val tool = sir.sf.Tool.find(_.name.equals(toolName)).map {
          case f if !addTool ⇒ f
          case _             ⇒ throw new IllegalArgumentException(s"the tool with name $toolName is already defined.")
        }.getOrElse {
          if (addTool)
            sir.sf.Tool.make(name = toolName)
          else
            throw new IllegalArgumentException(s"there is no tool with the name $toolName. Use --list-tools for details.")
        }

        // set description
        if (null != toolDescription) {
          tool.description = toolDescription
        }

        // remove targets, before inserting new ones to allow overwriting the exist one in a single pass
        if (dropTargets) {
          tool.buildTargets.clear()
        }

        // parse target string
        if (null != newTarget) {
          val parts = newTarget.split(' ')
          val pathImage = parts(1)
          val output = sir.sf.FilePath.make(
            isAbsolut = pathImage.charAt(0) == '/',
            parts = pathImage.split(File.pathSeparatorChar).filterNot(_.isEmpty).to
          )

          tool.buildTargets += sir.sf.BuildInformation.make(
            language = parts(0),
            output = output,
            options = parts.takeRight(parts.length - 2).to
          )
        }

        // set selected types
        if (null != selectTypes) {
          if (1 == selectTypes.length && selectTypes(0).equals("*")) {
            // select all
            tool.selectedUserTypes = sir.sf.UserdefinedType.to
          } else {
            ???
          }
        }

        // build a tool; this happens after all setup, so that it can be done in one call
        if (build) {
          sir.build(tool)
        }

      }

      // flush changes after everything could be performed successfully
      sir.sf.flush(ThrowException)
    }
  }
  val irParser = new scopt.OptionParser[IRConfig]("skillc <file.sir>") {
    head("skillc", BuildInfo.version, "(skill intermediate representation mode)")

    // master spec \\

    opt[File]("from-spec").optional().action(
      (p, c) ⇒ c.copy(fromSpec = p)
    ).text("create a new sir file from an argument specification overwriting a potentially preexisting one")

    // tools as a whole \\

    opt[Unit]("list-tools").optional().action(
      (p, c) ⇒ c.copy(listTools = true)
    ).text("print a list of all tools")

    opt[String]('t', "tool").optional().action(
      (p, c) ⇒ c.copy(toolName = p)
    ).text("select an existing tool")

    opt[String]("add-tool").optional().action(
      (p, c) ⇒ c.copy(toolName = p, addTool = true)
    ).text("add and select a new tool")

    // targets \\

    opt[Unit]("drop-all-targets").optional().action(
      (p, c) ⇒ c.copy(dropTargets = true)
    ).text("removes all targets")

    opt[String]("add-target").optional().action(
      (p, c) ⇒ c.copy(newTarget = p)
    ).text("add a new target: language outpath options...")

    // tool properties \\

    opt[String]("set-desc").optional().action(
      (p, c) ⇒ c.copy(toolDescription = p)
    ).text("set description for a selected tool")

    opt[Seq[String]]("select-types").optional().action(
      (p, c) ⇒ c.copy(selectTypes = p.to)
    ).text("select the argument list of types for the selected tool; use * for all types")

    // other \\

    opt[Unit]('b', "build").optional().action(
      (p, c) ⇒ c.copy(build = true)
    ).text("build selected tool")

    help("help").text("prints this usage text")
  }
}
