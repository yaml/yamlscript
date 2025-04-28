#pragma once
#ifndef YAMLSCRIPT_C_H__
#define YAMLSCRIPT_C_H__


#define YAMLSCRIPT_VERSION_STR "0.1.95"


/// error code enumeration
typedef enum {
    yamlscript_success = 0,  ///< success
    yamlscript_null_arg,     ///< (at least) one of the arguments is null
    yamlscript_bad_arg,      ///< (at least) one of the arguments is problematic
    yamlscript_lib_error,    ///< error resolving YS library
    yamlscript_bad_init,     ///< library initialized incorrectly
    yamlscript_ys_error,     ///< error parsing YS code
    yamlscript_small_buffer, ///< output buffer is too small
} yamlscript_errcode;


/// read-only buffer
typedef struct {
    const char * buf;
    int size;
} yamlscript_buffer_ro_t;


/// read-write buffer
typedef struct {
    char *buf;
    int size;
} yamlscript_buffer_rw_t;


/// an opaque handle to library data
typedef void* yamlscript_lib;


/** initialize the library */
yamlscript_errcode
yamlscript_init(yamlscript_lib* yslib);


/** terminate the library */
yamlscript_errcode
yamlscript_terminate(const yamlscript_lib yslib);


/** Convert YamlScript to JSON.
 *
 * @param yslib[IN] Library data obtained with @ref yamlscript_init()
 * @param ys[IN] Read-only input buffer containing YamlScript code
 * @param json[INOUT] Writeable output buffer where the JSON code is to be written
 * @param json_size[OUT] Number of characters written into the JSON buffer
 * @return error code, of type @ref yamlscript_errcode
 */
yamlscript_errcode
yamlscript_load_ys_to_json(const yamlscript_lib yslib,
                           yamlscript_buffer_ro_t const* ys,
                           yamlscript_buffer_rw_t const* json,
                           int *json_size);

#endif // YAMLSCRIPT_C_H__
