#define LIBYS_BUILD
#include "libys.h"
#include <stdint.h>
#include <stdlib.h>

/* Unlike cgo's special C.malloc wrapper, this can report allocation failure. */
void *ys_alloc(void) { return malloc(1); }

/* Attachment identity belongs to the calling native thread, not a Go worker. */
#ifdef _MSC_VER
#define YS_TLS __declspec(thread)
#else
#define YS_TLS __thread
#endif
static YS_TLS char owner;

extern int ys_create(void **, void **, uintptr_t);
extern int ys_attach(void *, void **, uintptr_t);
extern int ys_detach(void *, uintptr_t);
extern int ys_teardown(void *, uintptr_t);
extern void *ys_current(void *, uintptr_t);
extern void *ys_isolate(void *);
extern char *ys_evaluate(void *, char *);

int graal_create_isolate(graal_create_isolate_params_t *params,
                        graal_isolate_t **isolate,
                        graal_isolatethread_t **thread) {
    (void)params;
    return ys_create((void **)isolate, (void **)thread, (uintptr_t)&owner);
}
int graal_attach_thread(graal_isolate_t *isolate,
                        graal_isolatethread_t **thread) {
    return ys_attach(isolate, (void **)thread, (uintptr_t)&owner);
}
int graal_detach_thread(graal_isolatethread_t *thread) {
    return ys_detach(thread, (uintptr_t)&owner);
}
int graal_tear_down_isolate(graal_isolatethread_t *thread) {
    return ys_teardown(thread, (uintptr_t)&owner);
}
graal_isolatethread_t *graal_get_current_thread(graal_isolate_t *isolate) {
    return ys_current(isolate, (uintptr_t)&owner);
}
graal_isolate_t *graal_get_isolate(graal_isolatethread_t *thread) {
    return ys_isolate(thread);
}
char *load_ys_to_json(graal_isolatethread_t *thread, const char *source) {
    return ys_evaluate(thread, (char *)source);
}
