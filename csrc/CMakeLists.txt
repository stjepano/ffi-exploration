cmake_minimum_required(VERSION 3.20)
project(test-library C)

set(LIBNAME test)
set(LIBSRC library.c library.h)

add_library(${LIBNAME} SHARED ${LIBSRC})

set(EXENAME testprog)
set(EXESRC main.c)
add_executable(${EXENAME} ${EXESRC})
target_link_libraries(${EXENAME} PRIVATE ${LIBNAME})