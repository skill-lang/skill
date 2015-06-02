/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c

import scala.collection.JavaConversions._
import java.io.PrintWriter

/**
 * @note Makefiles require openRaw, because their comment stile differs from C; furthermore they require tab characters
 * which makes them ugly
 * @todo replace this absurd shit by cmake
 * @author Timm Felden
 */
trait MakefileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = openRaw("makefile")

    val prefix_capital = prefix.toUpperCase

    val tab = "\t"

    out.write(s"""CC=gcc
CFLAGS= -I/usr/include/glib-2.0 -I/usr/lib/x86_64-linux-gnu/glib-2.0/include -Wall -g3 -O3 -lglib-2.0 -lm -lpthread -lrt -fPIC -std=c99 -pedantic-errors

${prefix_capital}DEPENDENCIES += ./api/${prefix}api.o

${prefix_capital}DEPENDENCIES += ./io/${prefix}binary_reader.o
${prefix_capital}DEPENDENCIES += ./io/${prefix}binary_writer.o
${prefix_capital}DEPENDENCIES += ./io/${prefix}reader.o
${prefix_capital}DEPENDENCIES += ./io/${prefix}writer.o

${prefix_capital}DEPENDENCIES += ./model/${prefix}field_information.o
${prefix_capital}DEPENDENCIES += ./model/${prefix}skill_state.o
${prefix_capital}DEPENDENCIES += ./model/${prefix}string_access.o
${prefix_capital}DEPENDENCIES += ./model/${prefix}type_enum.o
${prefix_capital}DEPENDENCIES += ./model/${prefix}type_information.o
${prefix_capital}DEPENDENCIES += ./model/${prefix}type_declaration.o
${prefix_capital}DEPENDENCIES += ./model/${prefix}storage_pool.o
${prefix_capital}DEPENDENCIES += ./model/${prefix}types.o

SOURCE_LOCATIONS += -I.

so: $$(${prefix_capital}DEPENDENCIES)
${tab}$$(CC) -o lib${prefix}api.so $$(${prefix_capital}DEPENDENCIES) $$(CFLAGS) -shared
${tab}ar rcs lib${prefix}api.a $$(${prefix_capital}DEPENDENCIES)
${tab}cp api/${prefix}api.h ${prefix}api.h

clean:
${tab}rm -rf ./api/*.o
${tab}rm -rf ./io/*.o
${tab}rm -rf ./model/*.o
${tab}rm -rf ${prefix}api.h
${tab}rm -rf lib${prefix}api.so
${tab}rm -rf lib${prefix}api.a

.PHONY: so clean
""")

    out.close()
  }

  protected def openRaw(path : String) : PrintWriter;
}
