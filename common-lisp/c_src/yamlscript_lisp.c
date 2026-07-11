// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define YAMLSCRIPT_VERSION "0.2.26"

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
static char error_json[512];

static int check_dir(const char *dir, char *path, size_t size) {
  FILE *file;

  snprintf(path, size, "%s/%s", dir, LIBYS_NAME);
  file = fopen(path, "r");
  if (file == NULL) return 0;
  fclose(file);
  return 1;
}

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
  if (check_dir("../libys/lib", path, size)) return 1;

  home = getenv("HOME");
  if (home != NULL) {
    char dir[4096];
    snprintf(dir, sizeof(dir), "%s/.local/lib", home);
    if (check_dir(dir, path, size)) return 1;
  }

  return 0;
}

static int open_libys(void) {
  char path[4096];

  if (libys != NULL) return 1;
  if (!find_libys(path, sizeof(path))) return 0;

  libys = dlopen(path, RTLD_NOW);
  if (libys == NULL) return 0;

  create_isolate =
    (create_isolate_fn)dlsym(libys, "graal_create_isolate");
  tear_down_isolate =
    (tear_down_isolate_fn)dlsym(libys, "graal_tear_down_isolate");
  load_ys_to_json =
    (load_ys_to_json_fn)dlsym(libys, "load_ys_to_json");

  return create_isolate != NULL && tear_down_isolate != NULL &&
    load_ys_to_json != NULL;
}

char *yamlscript_load_json(const char *input) {
  void *thread = NULL;
  char *json;

  if (!open_libys()) {
    snprintf(error_json, sizeof(error_json),
      "{\"error\":{\"cause\":\"Shared library file '%s' not found\"}}",
      LIBYS_NAME);
    return error_json;
  }

  if (create_isolate(NULL, NULL, &thread) != 0) {
    return "{\"error\":{\"cause\":\"Failed to create isolate\"}}";
  }

  json = load_ys_to_json(thread, input);
  tear_down_isolate(thread);

  return json;
}

int main(void) {
  char *input = NULL;
  size_t cap = 0;
  size_t len = 0;
  int ch;
  char *json;

  while ((ch = getchar()) != EOF) {
    if (len + 1 >= cap) {
      cap = cap == 0 ? 4096 : cap * 2;
      input = realloc(input, cap);
      if (input == NULL) return 1;
    }
    input[len++] = (char)ch;
  }

  if (input == NULL) {
    input = calloc(1, 1);
    if (input == NULL) return 1;
  } else {
    input[len] = '\0';
  }

  json = yamlscript_load_json(input);
  free(input);

  if (json == NULL) return 1;
  fputs(json, stdout);
  return 0;
}
