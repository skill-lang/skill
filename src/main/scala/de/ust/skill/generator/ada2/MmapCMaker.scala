/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada2

import scala.collection.JavaConversions._

trait MmapCMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("mmap.c")

    out.write(s"""
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/stat.h>

typedef struct {
   FILE const *stream;
   size_t const length;
   unsigned char const *pointer;
} mmap_c_array;

void error (char const *message)
{
   fprintf (stderr, "mmap C: %s\n", message);
   exit (EXIT_FAILURE);
}

mmap_c_array mmap_open (char const *filename)
{
   FILE *stream = fopen (filename, "r");
   if (NULL == stream) error ("Execution of function fopen failed.");

   struct stat fileStat;
   if (-1 == fstat (fileno (stream), &fileStat)) error ("Execution of function fstat failed.");
   size_t const length = fileStat.st_size;

   char const *mapped = mmap (NULL, length, PROT_READ, MAP_PRIVATE, fileno (stream), 0);
   if (MAP_FAILED == mapped) error ("Execution of function mmap failed.");

   mmap_c_array const rval = { stream, length, mapped };
   return rval;
}

void mmap_close (FILE *stream)
{
   fclose (stream);
}
    """.replaceAll("""\h+$""", ""))

    out.close()
  }
}
