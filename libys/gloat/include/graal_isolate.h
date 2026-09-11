#ifndef LIBYS_GRAAL_ISOLATE_H
#define LIBYS_GRAAL_ISOLATE_H

#if defined(_WIN32) && defined(LIBYS_BUILD)
#define LIBYS_API __declspec(dllexport)
#else
#define LIBYS_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef struct graal_create_isolate_params_t graal_create_isolate_params_t;
typedef struct graal_isolate_t graal_isolate_t;
typedef struct graal_isolatethread_t graal_isolatethread_t;

LIBYS_API int graal_create_isolate(
    graal_create_isolate_params_t *params,
    graal_isolate_t **isolate,
    graal_isolatethread_t **thread);
LIBYS_API int graal_tear_down_isolate(graal_isolatethread_t *thread);
LIBYS_API int graal_attach_thread(
    graal_isolate_t *isolate,
    graal_isolatethread_t **thread);
LIBYS_API int graal_detach_thread(graal_isolatethread_t *thread);
LIBYS_API graal_isolatethread_t *graal_get_current_thread(graal_isolate_t *isolate);
LIBYS_API graal_isolate_t *graal_get_isolate(graal_isolatethread_t *thread);

#ifdef __cplusplus
}
#endif

#endif
