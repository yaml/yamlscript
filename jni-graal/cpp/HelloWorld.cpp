#include <jni.h>
#include <stdio.h>
#include <cstring>
#include "HelloWorld.h"

#if defined(__cplusplus)
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_HelloWorld_print(JNIEnv *env, jobject obj) {
        printf("Hello from the C++ shared library\n");
}

JNIEXPORT void JNICALL
Java_HelloWorld_print_1num_1plus_12(JNIEnv *env, jobject obj, jint num) {
        printf("The answer is %d\n", num + 2);
}

int no_problem(const char *src, int src_len,
                 /* */ char *dst, int dst_len)
{
    const char *success = "it_works!";
    const int suc_len = (int)strlen(success);
    const int req_len = suc_len + 2/* '*/ + src_len + 1/*'*/ + 1/*\0*/;
    if(req_len <= dst_len)
    {
        int ret = snprintf(
            dst,
            (size_t)dst_len,
            "%s '%.*s'",
            success,
            (int)src_len,
            src
        );
    }
    return req_len;
}

JNIEXPORT jint JNICALL
Java_HelloWorld_foo(JNIEnv *env, jobject, jbyteArray src, jint src_len, jbyteArray dst, jint dst_len) {

    jboolean src_is_copy, dst_is_copy;
    jbyte* src_ = env->GetByteArrayElements(src, &src_is_copy);
    jbyte* dst_ = env->GetByteArrayElements(dst, &dst_is_copy);
    int rc = no_problem((const char*)src_, src_len, (char*)dst_, dst_len);
    env->ReleaseByteArrayElements(src, src_, 0);
    env->ReleaseByteArrayElements(dst, dst_, 0);

    return rc;
}

#if defined(__cplusplus)
}
#endif
