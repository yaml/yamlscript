cmake_minimum_required(VERSION 3.12)
project(rapidyaml
    DESCRIPTION "rapidyaml -> yamlscript"
    HOMEPAGE_URL "https://github.com/biojppm/rapidyaml -> https://github.com/yaml/yamlscript"
    LANGUAGES CXX)

option(YS2EDN_TIMED "add timings to sections" OFF)

if(UNIX)
    set(CMAKE_SHARED_LIBRARY_SUFFIX .so)
endif()

add_library(rapidyaml
    rapidyaml_all.hpp
    rapidyaml_edn_handler.hpp
    rapidyaml_edn_handler.cpp
    rapidyaml_edn.hpp
    rapidyaml_edn.cpp
)
target_include_directories(rapidyaml PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}
    ${CMAKE_CURRENT_LIST_DIR}/rapidyaml/test
)
target_compile_definitions(rapidyaml PUBLIC
    RYML_WITH_TAB_TOKENS
    RYML_DEFAULT_CALLBACK_USES_EXCEPTIONS
    RYML_SINGLE_HEADER
    $<$<BOOL:${YS2EDN_TIMED}>:YS2EDN_TIMED>
)
set_target_properties(rapidyaml PROPERTIES CXX_STANDARD 17)

add_executable(rapidyaml-test rapidyaml_test.cpp)
target_link_libraries(rapidyaml-test rapidyaml)
