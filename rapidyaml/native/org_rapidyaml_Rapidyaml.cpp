#include <jni.h>
#include "./rapidyaml_edn.hpp"
#include <stdio.h>

#ifndef _Included_org_rapidyaml_Rapidyaml
#define _Included_org_rapidyaml_Rapidyaml
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     org_rapidyaml_Rapidyaml
 * Method:    ys2edn_init
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jlong JNICALL
Java_org_rapidyaml_Rapidyaml_ys2edn_1init(JNIEnv *env, jobject)
{
    Ryml2Edn *obj = ys2edn_init(env);
    return (jlong)obj; // ???
}

/*
 * Class:     org_rapidyaml_Rapidyaml
 * Method:    ys2edn_destroy
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL
Java_org_rapidyaml_Rapidyaml_ys2edn_1destroy(JNIEnv *, jobject, jlong obj)
{
    ys2edn_destroy((Ryml2Edn *)obj);
}

/*
 * Class:     org_rapidyaml_Rapidyaml
 * Method:    ys2edn
 * Signature: (Ljava/lang/Object;Ljava/lang/String;[BI[BI)I
 */
JNIEXPORT jint JNICALL
Java_org_rapidyaml_Rapidyaml_ys2edn(JNIEnv *env, jobject,
                                    jlong obj, jstring filename,
                                    jbyteArray src, jint src_len,
                                    jbyteArray dst, jint dst_len)
{
    (void)filename;
    jboolean src_is_copy, dst_is_copy;
    jbyte* src_ = env->GetByteArrayElements(src, &src_is_copy);
    jbyte* dst_ = env->GetByteArrayElements(dst, &dst_is_copy);
    int rc = ys2edn((Ryml2Edn*)obj, "foo",
                    (char*)src_, src_len,
                    (char*)dst_, dst_len);
    env->ReleaseByteArrayElements(src, src_, 0);
    env->ReleaseByteArrayElements(dst, dst_, 0);
    return rc;
}

/*
 * Class:     org_rapidyaml_Rapidyaml
 * Method:    ys2edn_retry_get
 * Signature: (Ljava/lang/Object;[BI)I
 */
JNIEXPORT jint JNICALL Java_org_rapidyaml_Rapidyaml_ys2edn_1retry_1get(JNIEnv *env, jobject,
                                                                       jlong obj,
                                                                       jbyteArray dst, jint dst_len)
{
    jboolean src_is_copy, dst_is_copy;
    jbyte* dst_ = env->GetByteArrayElements(dst, &dst_is_copy);
    int rc = ys2edn_retry_get((Ryml2Edn*)obj, (char*)dst_, dst_len);
    env->ReleaseByteArrayElements(dst, dst_, 0);
    return rc;
}

#ifdef __cplusplus
}
#endif
#endif
