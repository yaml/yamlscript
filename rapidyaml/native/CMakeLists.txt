cmake_minimum_required(VERSION 3.12)
project(ysparse
    DESCRIPTION "ysparse: rapidyaml -> yamlscript"
    HOMEPAGE_URL "https://github.com/biojppm/rapidyaml -> https://github.com/yaml/yamlscript"
    LANGUAGES CXX)

find_package(JNI REQUIRED)

option(YSPARSE_TIMED "add timings to sections" OFF)
option(YSPARSE_DBG "enable debug logs" OFF)

if(UNIX)
    set(CMAKE_SHARED_LIBRARY_SUFFIX .so)
endif()

set(libname rapidyaml) # TODO rename to ysparse

add_library(${libname}
    #
    # JNI bridge
    org_rapidyaml_Rapidyaml.h
    org_rapidyaml_Rapidyaml.cpp
    #
    # ysparse files
    ysparse_common.hpp
    ysparse_edn_handler.hpp
    ysparse_edn_handler.cpp
    ysparse_edn.hpp
    ysparse_edn.cpp
    ysparse_evt_handler.hpp
    ysparse_evt_handler.cpp
    ysparse_evt.hpp
    ysparse_evt.cpp
    #
    # files required from rapidyaml
    rapidyaml/src/c4/yml/common.cpp
    rapidyaml/src/c4/yml/node_type.cpp
    rapidyaml/src/c4/yml/parse.cpp
    rapidyaml/src/c4/yml/tree.cpp
    rapidyaml/src/c4/yml/tag.cpp
    rapidyaml/src/c4/yml/reference_resolver.cpp
    #
    # files required from rapidyaml/ext/c4core
    rapidyaml/ext/c4core/src/c4/base64.cpp
    rapidyaml/ext/c4core/src/c4/error.cpp
    rapidyaml/ext/c4core/src/c4/language.cpp
    rapidyaml/ext/c4core/src/c4/utf.cpp
)
target_include_directories(${libname} PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}
    ${CMAKE_CURRENT_LIST_DIR}/rapidyaml/src
    ${CMAKE_CURRENT_LIST_DIR}/rapidyaml/ext/c4core/src
)
target_compile_definitions(${libname} PUBLIC
    RYML_WITH_TAB_TOKENS
    RYML_DEFAULT_CALLBACK_USES_EXCEPTIONS
    $<$<BOOL:${YSPARSE_TIMED}>:YSPARSE_TIMED>
    $<$<BOOL:${YSPARSE_DBG}>:RYML_DBG>
)
set_target_properties(${libname} PROPERTIES CXX_STANDARD 17)

target_include_directories(${libname} PUBLIC ${JNI_INCLUDE_DIRS})

add_executable(${libname}-test ysparse_test.cpp)
target_link_libraries(${libname}-test ${libname})
add_custom_target(${libname}-test-run
    DEPENDS ${libname}-test
    COMMAND $<TARGET_FILE:${libname}-test>
    COMMENT "running C++ tests"
)
