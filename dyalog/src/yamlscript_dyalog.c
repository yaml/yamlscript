#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define YS_VERSION "0.2.31"

typedef int (*graal_create_isolate_fn)(void *, void *, void *);
typedef int (*graal_tear_down_isolate_fn)(void *);
typedef char *(*load_ys_to_json_fn)(void *, char *);

static void *libys = NULL;
static void *isolate_thread = NULL;
static graal_tear_down_isolate_fn tear_down_isolate = NULL;
static load_ys_to_json_fn load_ys = NULL;

static int copy_output(char *output, int max, const char *text) {
  int len = (int)strlen(text);
  if (max <= 0) {
    return -len;
  }
  if (len >= max) {
    memcpy(output, text, (size_t)max - 1);
    output[max - 1] = '\0';
    return -len;
  }
  memcpy(output, text, (size_t)len + 1);
  return len;
}

static int copy_error(char *output, int max, const char *message) {
  char buffer[4096];
  snprintf(
    buffer,
    sizeof(buffer),
    "{\"error\":{\"cause\":\"%s\"}}",
    message
  );
  return copy_output(output, max, buffer);
}

static int open_libys(char *output, int max) {
  if (load_ys != NULL) {
    return 0;
  }

  const char *names[] = {
    "libys.so." YS_VERSION,
    "libys.so",
    NULL,
  };

  const char *env = getenv("YAMLSCRIPT_DYALOG_LIBYS");
  if (env != NULL) {
    libys = dlopen(env, RTLD_LAZY);
  }

  for (int i = 0; names[i] != NULL; i++) {
    if (libys != NULL) {
      break;
    }
    libys = dlopen(names[i], RTLD_LAZY);
    if (libys != NULL) {
      break;
    }
  }

  if (libys == NULL) {
    return copy_error(output, max, dlerror());
  }

  graal_create_isolate_fn create_isolate =
    (graal_create_isolate_fn)dlsym(libys, "graal_create_isolate");
  tear_down_isolate =
    (graal_tear_down_isolate_fn)dlsym(libys, "graal_tear_down_isolate");
  load_ys = (load_ys_to_json_fn)dlsym(libys, "load_ys_to_json");

  if (
    create_isolate == NULL ||
    tear_down_isolate == NULL ||
    load_ys == NULL
  ) {
    return copy_error(output, max, "required libys symbol not found");
  }

  if (create_isolate(NULL, NULL, &isolate_thread) != 0) {
    return copy_error(output, max, "failed to create GraalVM isolate");
  }

  return 0;
}

int ys_load_json(const char *input, char *output, int max) {
  int rc = open_libys(output, max);
  if (rc != 0) {
    return rc;
  }

  char *json = load_ys(isolate_thread, (char *)input);
  if (json == NULL) {
    return copy_error(output, max, "null response from libys");
  }

  return copy_output(output, max, json);
}

int ys_close(void) {
  if (tear_down_isolate == NULL || isolate_thread == NULL) {
    return 0;
  }
  int rc = tear_down_isolate(isolate_thread);
  isolate_thread = NULL;
  return rc;
}
