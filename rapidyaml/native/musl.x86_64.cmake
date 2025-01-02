set(MUSL_DIR $ENV{MUSL_DIR})
set(MUSL_TGT x86_64-linux-musl)

set(MUSL_BIN ${MUSL_DIR}/bin/${MUSL_TGT}-)
set(MUSL_PFX ${MUSL_DIR}/${MUSL_TGT})
set(MUSL_LIB ${MUSL_DIR}/${MUSL_TGT}/lib)
set(MUSL_INC ${MUSL_DIR}/${MUSL_TGT}/include)

set(MUSL TRUE)

set(CMAKE_C_COMPILER          ${MUSL_BIN}gcc)
set(CMAKE_CXX_COMPILER        ${MUSL_BIN}g++)
set(CMAKE_AR                  ${MUSL_BIN}ar)
set(CMAKE_C_COMPILER_AR       ${MUSL_BIN}ar)
set(CMAKE_CXX_COMPILER_AR     ${MUSL_BIN}ar)
set(CMAKE_RANLIB              ${MUSL_BIN}ranlib)
set(CMAKE_C_COMPILER_RANLIB   ${MUSL_BIN}ranlib)
set(CMAKE_CXX_COMPILER_RANLIB ${MUSL_BIN}ranlib)
set(CMAKE_ADDR2LINE           ${MUSL_BIN}addr2line)
set(CMAKE_LINKER              ${MUSL_BIN}ld)
set(CMAKE_NM                  ${MUSL_BIN}nm)
set(CMAKE_OBJCOPY             ${MUSL_BIN}objcopy)
set(CMAKE_OBJDUMP             ${MUSL_BIN}objdump)
set(CMAKE_READELF             ${MUSL_BIN}readelf)
set(CMAKE_STRIP               ${MUSL_BIN}strip)

# set searching rules for cross-compiler
set(CMAKE_SYSTEM_PREFIX_PATH  ${MUSL_PFX})
set(CMAKE_SYSTEM_LIBRARY_PATH ${MUSL_LIB})
set(CMAKE_SYSTEM_INCLUDE_PATH ${MUSL_INC})
set(CMAKE_FIND_ROOT_PATH      ${MUSL_PFX})
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM BOTH) # search also in the host
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH) # search also in the host
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE BOTH) # search also in the host
