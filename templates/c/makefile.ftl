CC=gcc
CFLAGS= -I/usr/include/glib-2.0 -I/usr/lib/x86_64-linux-gnu/glib-2.0/include  -lglib-2.0 -lm -lpthread -lrt -fPIC -std=c99 -pedantic-errors

<#-- TODO compile with '-pedantic-errors' -->

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

${prefix_capital}SOURCE_LOCATIONS += -I. 

${prefix}so: $(${prefix_capital}DEPENDENCIES)
${"\t"}$(CC) -o lib${prefix}api.so $(${prefix_capital}DEPENDENCIES) $(CFLAGS) -shared
${"\t"}ar rcs lib${prefix}api.a $(${prefix_capital}DEPENDENCIES)
${"\t"}cp api/${prefix}api.h ${prefix}api.h

clean:
${"\t"}rm -rf ./api/*.o
${"\t"}rm -rf ./io/*.o
${"\t"}rm -rf ./model/*.o
${"\t"}rm -rf ${prefix}api.h
${"\t"}rm -rf lib${prefix}api.so
${"\t"}rm -rf lib${prefix}api.a

.PHONY: ${prefix}so clean