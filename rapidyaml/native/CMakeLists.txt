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


add_library(ysparse
    #
    # JNI bridge
    org_rapidyaml_Rapidyaml.h
    org_rapidyaml_Rapidyaml.cpp
    #
    # ysparse files
    ysparse_common.hpp
    ysparse_evt.hpp
    ysparse_evt.cpp
    #
    # files required from rapidyaml
    rapidyaml/src/c4/yml/common.hpp
    rapidyaml/src/c4/yml/common.cpp
    rapidyaml/src/c4/yml/node_type.hpp
    rapidyaml/src/c4/yml/node_type.cpp
    rapidyaml/src/c4/yml/tag.cpp
    rapidyaml/src_extra/c4/yml/extra/event_handler_ints.hpp
    rapidyaml/src_extra/c4/yml/extra/event_handler_ints.cpp
    #
    # files required from rapidyaml/ext/c4core
    rapidyaml/ext/c4core/src/c4/substr.hpp
    rapidyaml/ext/c4core/src/c4/base64.hpp
    rapidyaml/ext/c4core/src/c4/base64.cpp
    rapidyaml/ext/c4core/src/c4/error.hpp
    rapidyaml/ext/c4core/src/c4/error.cpp
    rapidyaml/ext/c4core/src/c4/language.hpp
    rapidyaml/ext/c4core/src/c4/language.cpp
    rapidyaml/ext/c4core/src/c4/utf.hpp
    rapidyaml/ext/c4core/src/c4/utf.cpp
)
target_include_directories(ysparse PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}
    ${CMAKE_CURRENT_LIST_DIR}/rapidyaml/src
    ${CMAKE_CURRENT_LIST_DIR}/rapidyaml/src_extra
    ${CMAKE_CURRENT_LIST_DIR}/rapidyaml/ext/c4core/src
)
target_compile_definitions(ysparse PUBLIC
    RYML_WITH_TAB_TOKENS
    RYML_DEFAULT_CALLBACK_USES_EXCEPTIONS
    $<$<BOOL:${YSPARSE_TIMED}>:YSPARSE_TIMED>
    $<$<BOOL:${YSPARSE_DBG}>:RYML_DBG>
)
set_target_properties(ysparse PROPERTIES CXX_STANDARD 17)

target_include_directories(ysparse PUBLIC ${JNI_INCLUDE_DIRS})

add_executable(ysparse-test
    ysparse_test.cpp
    rapidyaml/src_extra/c4/yml/extra/ints_utils.hpp
    rapidyaml/src_extra/c4/yml/extra/ints_utils.cpp
    rapidyaml/src_extra/c4/yml/extra/scalar.cpp
)
target_link_libraries(ysparse-test ysparse)
add_custom_target(ysparse-test-run
    DEPENDS ysparse-test
    COMMAND $<TARGET_FILE:ysparse-test>
    COMMENT "running C++ tests"
)
add_custom_target(ysparse-test-run-timing
    DEPENDS ysparse-test
    COMMAND $<TARGET_FILE:ysparse-test> --timing
    COMMENT "running C++ tests, with timing"
)
