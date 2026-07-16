#include "moonbit.h"

#include <dlfcn.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define YS_VERSION "0.2.28"

typedef int (*graal_create_isolate_fn)(void *, void *, void *);
typedef int (*graal_tear_down_isolate_fn)(void *);
typedef char *(*load_ys_to_json_fn)(void *, char *);

static void *libys = NULL;
static void *isolate_thread = NULL;
static load_ys_to_json_fn load_ys = NULL;
static graal_tear_down_isolate_fn tear_down_isolate = NULL;

static void fail(const char *message) {
  fprintf(stderr, "YAMLScript MoonBit binding error: %s\n", message);
  abort();
}

static void open_libys(void) {
  if (libys != NULL) {
    return;
  }

  const char *names[] = {
#ifdef __APPLE__
    "libys.dylib." YS_VERSION,
    "libys.dylib",
#else
    "libys.so." YS_VERSION,
    "libys.so",
#endif
    NULL,
  };

  for (int i = 0; names[i] != NULL; i++) {
    libys = dlopen(names[i], RTLD_LAZY);
    if (libys != NULL) {
      break;
    }
  }

  if (libys == NULL) {
    fail(dlerror());
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
    fail("required libys symbol not found");
  }

  if (create_isolate(NULL, NULL, &isolate_thread) != 0) {
    fail("failed to create GraalVM isolate");
  }
}

moonbit_bytes_t ys_load_ys_to_json(moonbit_bytes_t input) {
  open_libys();

  int32_t len = Moonbit_array_length(input);
  char *source = (char *)malloc((size_t)len + 1);
  if (source == NULL) {
    fail("failed to allocate input buffer");
  }

  memcpy(source, input, (size_t)len);
  source[len] = '\0';

  char *json = load_ys(isolate_thread, source);
  free(source);

  if (json == NULL) {
    fail("null response from libys");
  }

  int32_t out_len = (int32_t)strlen(json);
  moonbit_bytes_t output = moonbit_make_bytes_raw(out_len);
  memcpy(output, json, (size_t)out_len);
  return output;
}
