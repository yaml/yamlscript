cmake_minimum_required(VERSION 3.12)
project(HelloWorld)

find_package(JNI REQUIRED)

add_library(HelloWorld
    HelloWorld.h
    HelloWorld.cpp
)

# target_link_libraries(HelloWorld PUBLIC JNI::JNI)
target_include_directories(HelloWorld PUBLIC ${JNI_INCLUDE_DIRS})
