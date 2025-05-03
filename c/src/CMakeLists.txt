cmake_minimum_required(VERSION 3.12)
project(yamlscript_c
    DESCRIPTION "yamlscript bindings for c"
    HOMEPAGE_URL "https://github.com/yaml/yamlscript"
    LANGUAGES C)

add_library(yamlscript_c
    yamlscript_c.c
    yamlscript_c.h
)
if(BUILD_SHARED_LIBS)
    target_compile_definitions(yamlscript_c PUBLIC
        YAMLSCRIPT_SHARED
        $<BUILD_INTERFACE:YAMLSCRIPT_EXPORTS>
    )
endif()
