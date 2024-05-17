cmake_minimum_required(VERSION 3.12)
project(rapidyaml
    DESCRIPTION "rapidyaml -> yamlscript"
    HOMEPAGE_URL "https://github.com/biojppm/rapidyaml -> https://github.com/yaml/yamlscript"
    LANGUAGES CXX)

add_library(rapidyaml
    ryml_all.hpp
    rapidyaml_edn_handler.hpp
    rapidyaml_edn_handler.cpp
    rapidyaml_events.hpp
    rapidyaml_events.cpp
)
target_include_directories(rapidyaml PUBLIC
    ${CMAKE_CURRENT_LIST_DIR}
    ${CMAKE_CURRENT_LIST_DIR}/rapidyaml/test
)
target_compile_definitions(rapidyaml PUBLIC
    RYML_WITH_TAB_TOKENS
    RYML_DEFAULT_CALLBACK_USES_EXCEPTIONS
    RYML_SINGLE_HEADER
)

add_executable(rapidyaml-test rapidyaml_test.cpp)
target_link_libraries(rapidyaml-test rapidyaml)
