#ifndef LIBYS_HEADER
#define LIBYS_HEADER "libys.h"
#endif
#include LIBYS_HEADER
#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <pthread.h>
#endif

static graal_isolate_t *shared;

static void evaluate(graal_isolatethread_t *thread) {
    char *json = load_ys_to_json(thread, "!ys-0\n6 * 7\n");
    assert(json && strstr(json, "\"data\":42"));
    free(json);
}

#ifdef _WIN32
static DWORD WINAPI worker(void *unused) {
#else
static void *worker(void *unused) {
#endif
    (void)unused;
    graal_isolatethread_t *thread = NULL;
    assert(graal_get_current_thread(shared) == NULL);
    assert(graal_attach_thread(shared, &thread) == 0 && thread);
    assert(graal_get_current_thread(shared) == thread);
    for (int i = 0; i < 10; ++i) evaluate(thread);
    assert(graal_detach_thread(thread) == 0);
    assert(graal_get_current_thread(shared) == NULL);
    return 0;
}

int main(void) {
    graal_isolate_t *isolate = NULL;
    graal_isolatethread_t *thread = NULL, *attached = NULL;
    graal_isolatethread_t *invalid = (void *)(uintptr_t)1;
    assert(graal_create_isolate(NULL, NULL, NULL) != 0);
    assert(graal_detach_thread(invalid) != 0);
    assert(graal_tear_down_isolate(invalid) != 0);
    assert(graal_get_isolate(invalid) == NULL);
    char *error = load_ys_to_json(invalid, "answer: 42");
    assert(error && strstr(error, "\"error\""));
    free(error);
    assert(graal_attach_thread(NULL, &thread) != 0 && !thread);
    assert(graal_create_isolate(NULL, NULL, &thread) == 0 && thread);
    isolate = graal_get_isolate(thread);
    assert(isolate && graal_get_current_thread(isolate) == thread);
    assert(graal_attach_thread(isolate, &attached) == 0 && attached == thread);
    evaluate(thread);
    error = load_ys_to_json(thread, "!ys-0\nmissing-function()\n");
    assert(error && strstr(error, "\"error\""));
    free(error);
    assert(graal_detach_thread(thread) == 0);
    assert(graal_get_isolate(thread) == NULL);
    assert(graal_detach_thread(thread) != 0);
    assert(graal_get_current_thread(isolate) == NULL);
    assert(graal_attach_thread(isolate, &thread) == 0 && thread);
    assert(graal_tear_down_isolate(thread) == 0);
    assert(graal_get_isolate(thread) == NULL);
    assert(graal_attach_thread(isolate, &thread) != 0 && !thread);

    assert(graal_create_isolate(NULL, &shared, NULL) == 0 && shared);
    thread = graal_get_current_thread(shared);
    assert(thread);
#ifdef _WIN32
    HANDLE workers[4];
    for (int i = 0; i < 4; ++i) {
        workers[i] = CreateThread(NULL, 0, worker, NULL, 0, NULL);
        assert(workers[i]);
    }
    assert(WaitForMultipleObjects(4, workers, TRUE, INFINITE) == WAIT_OBJECT_0);
    for (int i = 0; i < 4; ++i) CloseHandle(workers[i]);
#else
    pthread_t workers[4];
    for (int i = 0; i < 4; ++i)
        assert(pthread_create(&workers[i], NULL, worker, NULL) == 0);
    for (int i = 0; i < 4; ++i)
        assert(pthread_join(workers[i], NULL) == 0);
#endif
    evaluate(thread);
    assert(graal_tear_down_isolate(thread) == 0);
    for (int i = 0; i < 100; ++i) {
        assert(graal_create_isolate(NULL, &isolate, &thread) == 0);
        assert(isolate && thread);
        assert(graal_tear_down_isolate(thread) == 0);
    }
    puts("libys C ABI tests passed");
    return 0;
}
