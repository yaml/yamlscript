#pragma once
#ifndef YAMLSCRIPT_C_H__
#define YAMLSCRIPT_C_H__


#define YAMLSCRIPT_VERSION_STR "0.1.95"
#ifdef _WIN32
    #ifdef YAMLSCRIPT_SHARED
        #ifdef YAMLSCRIPT_EXPORTS
            #define YAMLSCRIPT_EXPORT __declspec(dllexport)
        #else
            #define YAMLSCRIPT_EXPORT __declspec(dllimport)
        #endif
    #else
        #define YAMLSCRIPT_EXPORT
    #endif
#else
    #define YAMLSCRIPT_EXPORT
#endif


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


/** Convert YamlScript to JSON.
 *
 * @param yslib[IN] Library data obtained with @ref yamlscript_init()
 * @param ys[IN] Read-only input buffer containing YamlScript code
 * @param json[INOUT] Writeable output buffer where the JSON code is to be written
 * @param json_size[IN] Size of the output buffer
 * @param json_size[OUT] Size required for the output buffer
 * @return error code, of type @ref yamlscript_errcode
 */
YAMLSCRIPT_EXPORT yamlscript_errcode
yamlscript_load_ys_to_json(const char *ys,
                           char *json,
                           int json_size,
                           int *json_size_required);

#endif // YAMLSCRIPT_C_H__
