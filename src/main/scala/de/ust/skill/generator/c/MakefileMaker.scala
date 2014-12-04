/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
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

    val tab = "\t"

    out.write(s"""CC=gcc
CFLAGS= -I/usr/include/glib-2.0 -I/usr/lib/x86_64-linux-gnu/glib-2.0/include -Wall -g3 -O3 -lglib-2.0 -lm -lpthread -lrt -fPIC -std=c99 -pedantic-errors

DEPENDENCIES += ./api/api.o

DEPENDENCIES += ./io/binary_reader.o
DEPENDENCIES += ./io/binary_writer.o
DEPENDENCIES += ./io/reader.o
DEPENDENCIES += ./io/writer.o

DEPENDENCIES += ./model/field_information.o
DEPENDENCIES += ./model/skill_state.o
DEPENDENCIES += ./model/string_access.o
DEPENDENCIES += ./model/type_enum.o
DEPENDENCIES += ./model/type_information.o
DEPENDENCIES += ./model/type_declaration.o
DEPENDENCIES += ./model/storage_pool.o
DEPENDENCIES += ./model/types.o

SOURCE_LOCATIONS += -I.

so: $$(DEPENDENCIES)
${tab}$$(CC) -o libapi.so $$(DEPENDENCIES) $$(CFLAGS) -shared
${tab}ar rcs libapi.a $$(DEPENDENCIES)
${tab}cp api/api.h api.h

clean:
${tab}rm -rf ./api/*.o
${tab}rm -rf ./io/*.o
${tab}rm -rf ./model/*.o
${tab}rm -rf api.h
${tab}rm -rf libapi.so
${tab}rm -rf libapi.a

.PHONY: so clean
""")

    out.close()
  }

  protected def openRaw(path : String) : PrintWriter;
}
