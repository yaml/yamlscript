// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

// C shim for the R yamlscript package.
//
// Loads the libys shared library at first use and exposes its
// load_ys_to_json function to R via the .Call interface. The search
// paths and exact version pinning are ported from the Python
// reference implementation.

#ifdef _WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <R.h>
#include <Rinternals.h>

// This value is automatically updated by 'make bump'.
// We currently only support binding to an exact version of libys:
#define YAMLSCRIPT_VERSION "0.2.26"

#ifdef _WIN32
#define LIBYS_NAME "libys.dll"
#elif defined(__APPLE__)
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
// Search LD_LIBRARY_PATH entries (PATH on Windows), then common
// install locations:
static int find_libys(char *path, size_t size) {
#ifdef _WIN32
  const char *library_path = getenv("PATH");
  const char *sep = ";";
#else
  const char *library_path = getenv("LD_LIBRARY_PATH");
  const char *sep = ":";
#endif
  const char *home;

  if (library_path != NULL) {
    char *paths = strdup(library_path);
    char *dir = strtok(paths, sep);
    while (dir != NULL) {
      if (check_dir(dir, path, size)) {
        free(paths);
        return 1;
      }
      dir = strtok(NULL, sep);
    }
    free(paths);
  }

#ifdef _WIN32
  home = getenv("USERPROFILE");
#else
  if (check_dir("/usr/local/lib", path, size)) return 1;

  home = getenv("HOME");
#endif
  if (home != NULL) {
    char dir[4096];
    snprintf(dir, sizeof(dir), "%s/.local/lib", home);
    if (check_dir(dir, path, size)) return 1;
  }

  return 0;
}

// Open libys and resolve the 3 symbols used by this binding:
static void open_libys(void) {
  char path[4096];

  if (libys != NULL) return;

  if (!find_libys(path, sizeof(path))) {
    Rf_error(
      "Shared library file '%s' not found\n"
      "Try: curl https://yamlscript.org/install |"
      " VERSION=%s LIB=1 bash\n"
      "See: https://github.com/yaml/yamlscript/wiki/"
      "Installing-YAMLScript",
      LIBYS_NAME, YAMLSCRIPT_VERSION);
  }

#ifdef _WIN32
  libys = (void *)LoadLibraryA(path);
#else
  libys = dlopen(path, RTLD_NOW);
#endif
  if (libys == NULL) {
    Rf_error("Failed to load shared library '%s'", path);
  }

#ifdef _WIN32
#define LIBYS_SYM(name) ((void *)GetProcAddress((HMODULE)libys, name))
#else
#define LIBYS_SYM(name) (dlsym(libys, name))
#endif

  create_isolate =
    (create_isolate_fn)LIBYS_SYM("graal_create_isolate");
  tear_down_isolate =
    (tear_down_isolate_fn)LIBYS_SYM("graal_tear_down_isolate");
  load_ys_to_json =
    (load_ys_to_json_fn)LIBYS_SYM("load_ys_to_json");

  if (create_isolate == NULL || tear_down_isolate == NULL ||
      load_ys_to_json == NULL) {
    Rf_error("Required symbols not found in libys");
  }
}

// Compile and eval a YAMLScript string, returning the raw JSON
// response string. A GraalVM isolate is created and torn down per
// call (like the Java binding):
SEXP C_yamlscript_load(SEXP input) {
  void *isolate = NULL;
  void *thread = NULL;
  const char *json;
  SEXP result;

  open_libys();

  if (create_isolate(NULL, &isolate, &thread) != 0) {
    Rf_error("Failed to create isolate");
  }

  json = load_ys_to_json(
    thread, CHAR(STRING_ELT(input, 0)));

  result = Rf_mkString(json == NULL ? "" : json);

  if (tear_down_isolate(thread) != 0) {
    Rf_error("Failed to tear down isolate");
  }

  return result;
}
