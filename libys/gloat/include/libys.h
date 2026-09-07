#ifndef LIBYS_H
#define LIBYS_H

#include "graal_isolate.h"

#ifdef __cplusplus
extern "C" {
#endif

char *load_ys_to_json(
  graal_isolatethread_t *thread, const char *yamlscript);

#ifdef __cplusplus
}
#endif

#endif
