#include <jni.h>

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
JNIEXPORT jobject JNICALL Java_org_rapidyaml_Rapidyaml_ys2edn_1init
  (JNIEnv *, jobject);

/*
 * Class:     org_rapidyaml_Rapidyaml
 * Method:    ys2edn_destroy
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_rapidyaml_Rapidyaml_ys2edn_1destroy
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_rapidyaml_Rapidyaml
 * Method:    ys2edn
 * Signature: (Ljava/lang/Object;Ljava/lang/String;[BI[BI)I
 */
JNIEXPORT jint JNICALL Java_org_rapidyaml_Rapidyaml_ys2edn
  (JNIEnv *, jobject, jobject, jstring, jbyteArray, jint, jbyteArray, jint);

/*
 * Class:     org_rapidyaml_Rapidyaml
 * Method:    ys2edn_retry_get
 * Signature: (Ljava/lang/Object;[BI)I
 */
JNIEXPORT jint JNICALL Java_org_rapidyaml_Rapidyaml_ys2edn_1retry_1get
  (JNIEnv *, jobject, jobject, jbyteArray, jint);

#ifdef __cplusplus
}
#endif
#endif
