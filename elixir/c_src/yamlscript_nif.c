// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

// Erlang NIF shim for the Elixir yamlscript binding.
//
// The BEAM cannot call arbitrary C functions directly, so this small
// NIF dlopens the libys shared library and exposes its
// load_ys_to_json function as a dirty-CPU NIF. The library search
// paths and exact version pinning are ported from the Python
// reference implementation.
//
// A GraalVM isolate is created and torn down per call (the same
// pattern as the Java and R bindings), which keeps the NIF safe to
// call from any BEAM scheduler thread.

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <erl_nif.h>

// This value is automatically updated by 'make bump'.
// We currently only support binding to an exact version of libys:
#define YAMLSCRIPT_VERSION "0.2.31"

#ifdef __APPLE__
#define LIBYS_NAME "libys.dylib." YAMLSCRIPT_VERSION
#else
#define LIBYS_NAME "libys.so." YAMLSCRIPT_VERSION
#endif

typedef int (*create_isolate_fn)(void *, void **, void **);
typedef int (*tear_down_isolate_fn)(void *);
typedef char *(*load_ys_to_json_fn)(void *, const char *);

static void *libys = NULL;
static create_isolate_fn create_isolate;
static tear_down_isolate_fn tear_down_isolate;
static load_ys_to_json_fn load_ys_to_json;
static char load_error[512] = "";

// Return 1 if the libys file exists in dir and fills path:
static int check_dir(const char *dir, char *path, size_t size) {
  FILE *file;

  snprintf(path, size, "%s/%s", dir, LIBYS_NAME);
  file = fopen(path, "r");
  if (file == NULL) return 0;
  fclose(file);
  return 1;
}

// Find the libys shared library file path.
// Search LD_LIBRARY_PATH entries, then common install locations:
static int find_libys(char *path, size_t size) {
  const char *library_path = getenv("LD_LIBRARY_PATH");
  const char *home;

  if (library_path != NULL) {
    char *paths = strdup(library_path);
    char *dir = strtok(paths, ":");
    while (dir != NULL) {
      if (check_dir(dir, path, size)) {
        free(paths);
        return 1;
      }
      dir = strtok(NULL, ":");
    }
    free(paths);
  }

  if (check_dir("/usr/local/lib", path, size)) return 1;

  home = getenv("HOME");
  if (home != NULL) {
    char dir[4096];
    snprintf(dir, sizeof(dir), "%s/.local/lib", home);
    if (check_dir(dir, path, size)) return 1;
  }

  return 0;
}

// Open libys and resolve the 3 symbols used by this binding.
// On failure, load_error is set and libys stays NULL:
static void open_libys(void) {
  char path[4096];

  if (!find_libys(path, sizeof(path))) {
    snprintf(load_error, sizeof(load_error),
      "Shared library file '%s' not found\n"
      "Try: curl https://yamlscript.org/install |"
      " VERSION=%s LIB=1 bash\n"
      "See: https://github.com/yaml/yamlscript/wiki/"
      "Installing-YAMLScript",
      LIBYS_NAME, YAMLSCRIPT_VERSION);
    return;
  }

  libys = dlopen(path, RTLD_NOW);
  if (libys == NULL) {
    snprintf(load_error, sizeof(load_error),
      "Failed to load shared library '%s'", path);
    return;
  }

  create_isolate =
    (create_isolate_fn)dlsym(libys, "graal_create_isolate");
  tear_down_isolate =
    (tear_down_isolate_fn)dlsym(libys, "graal_tear_down_isolate");
  load_ys_to_json =
    (load_ys_to_json_fn)dlsym(libys, "load_ys_to_json");

  if (create_isolate == NULL || tear_down_isolate == NULL ||
      load_ys_to_json == NULL) {
    snprintf(load_error, sizeof(load_error),
      "Required symbols not found in libys");
    dlclose(libys);
    libys = NULL;
  }
}

// Build an {:error, binary} tuple:
static ERL_NIF_TERM error_tuple(ErlNifEnv *env, const char *message) {
  ErlNifBinary bin;
  size_t len = strlen(message);

  enif_alloc_binary(len, &bin);
  memcpy(bin.data, message, len);
  return enif_make_tuple2(env,
    enif_make_atom(env, "error"),
    enif_make_binary(env, &bin));
}

// Compile and eval a YAMLScript string, returning the raw JSON
// response as a binary, or {:error, binary} if libys is unusable:
static ERL_NIF_TERM load_ys_to_json_nif(
  ErlNifEnv *env, int argc, const ERL_NIF_TERM argv[]
) {
  ErlNifBinary input, output;
  char *input_z;
  const char *json;
  void *isolate = NULL;
  void *thread = NULL;
  size_t len;

  if (argc != 1 || !enif_inspect_binary(env, argv[0], &input)) {
    return enif_make_badarg(env);
  }

  if (libys == NULL) {
    return error_tuple(env, load_error);
  }

  // Null-terminate the input binary:
  input_z = enif_alloc(input.size + 1);
  memcpy(input_z, input.data, input.size);
  input_z[input.size] = '\0';

  if (create_isolate(NULL, &isolate, &thread) != 0) {
    enif_free(input_z);
    return error_tuple(env, "Failed to create isolate");
  }

  json = load_ys_to_json(thread, input_z);
  enif_free(input_z);

  if (json == NULL) {
    tear_down_isolate(thread);
    return error_tuple(env, "Null response from 'libys'");
  }

  len = strlen(json);
  enif_alloc_binary(len, &output);
  memcpy(output.data, json, len);

  if (tear_down_isolate(thread) != 0) {
    enif_release_binary(&output);
    return error_tuple(env, "Failed to tear down isolate");
  }

  return enif_make_binary(env, &output);
}

static int load(
  ErlNifEnv *env, void **priv_data, ERL_NIF_TERM load_info
) {
  (void)env;
  (void)priv_data;
  (void)load_info;
  open_libys();
  return 0;
}

static ErlNifFunc nif_funcs[] = {
  {"nif_load_ys_to_json", 1, load_ys_to_json_nif,
   ERL_NIF_DIRTY_JOB_CPU_BOUND},
};

ERL_NIF_INIT(Elixir.YAMLScript, nif_funcs, load, NULL, NULL, NULL)
