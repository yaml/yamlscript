cmake_minimum_required(VERSION 3.12)
project(yamlscript_c_test
    HOMEPAGE_URL "https://github.com/yaml/yamlscript"
    LANGUAGES C)

# FIXME
find_path(YAMLSCRIPT_C_INC yamlscript_c.h REQUIRED HINTS ../src)
find_library(YAMLSCRIPT_C_LIB yamlscript_c REQUIRED HINTS ../src/build)
add_library(yamlscript_c INTERFACE)
target_include_directories(yamlscript_c INTERFACE ${YAMLSCRIPT_C_INC})
target_link_libraries(yamlscript_c INTERFACE ${YAMLSCRIPT_C_LIB})

add_executable(yamlscript_c_test test.c)
target_link_libraries(yamlscript_c_test PUBLIC yamlscript_c)

add_custom_target(test-run
    COMMAND $<TARGET_FILE:yamlscript_c_test>
    DEPENDS yamlscript_c_test
)
