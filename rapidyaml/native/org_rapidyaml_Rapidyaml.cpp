#include <jni.h>
#include "ysparse_evt.hpp"

#ifdef __cplusplus
extern "C" {
#endif


static C4_NO_INLINE void throw_runtime_exception(JNIEnv * env, const char* msg);
static C4_NO_INLINE void throw_parse_error(JNIEnv *env, size_t offset, size_t line, size_t column, const char *msg);


JNIEXPORT void JNICALL
Java_org_rapidyaml_Rapidyaml_ysparse_1timing_1set(JNIEnv *, jobject, jboolean yes)
{
    ysparse_timing_set(yes);
}

JNIEXPORT jlong JNICALL
Java_org_rapidyaml_Rapidyaml_ysparse_1init(JNIEnv *env, jobject)
{
    ysparse *obj = ysparse_init();
    return (jlong)obj;
}


JNIEXPORT void JNICALL
Java_org_rapidyaml_Rapidyaml_ysparse_1destroy(JNIEnv *, jobject, jlong obj)
{
    ysparse_destroy((ysparse*)obj);
}


JNIEXPORT jint JNICALL
Java_org_rapidyaml_Rapidyaml_ysparse_1parse(JNIEnv *env, jobject,
                                           jlong obj, jstring jfilename,
                                           jbyteArray src, jint src_len,
                                           jintArray dst, jint dst_len)
{
    TIMED_SECTION("jni:ysparse", (size_type)src_len);
    jbyte* src_ = nullptr;
    int* dst_ = nullptr;
    const char *filename = nullptr;
    jboolean dst_is_copy = false;
    jboolean src_is_copy = false;
    {
        TIMED_SECTION("jni:ysparse/get_jni", (size_type)src_len);
        // this is __S__L__O__W__
        // https://stackoverflow.com/questions/43763129/jni-is-getintarrayelements-always-linear-in-time
        // https://stackoverflow.com/questions/7395695/how-to-convert-from-bytebuffer-to-integer-and-string
        {
            TIMED_SECTION("jni:ysparse/GetByteArray(src)");
            src_ = env->GetByteArrayElements(src, &src_is_copy);
        }
        {
            TIMED_SECTION("jni:ysparse/GetIntArray(dst)");
            dst_ = env->GetIntArrayElements(dst, &dst_is_copy);
        }
        {
            TIMED_SECTION("jni:ysparse/GetStringUTFChars()");
            filename = env->GetStringUTFChars(jfilename, 0);
        }
    }
    int rc = 0;
    {
        TIMED_SECTION("jni:ysparse/parse", (size_type)src_len);
        try
        {
            rc = ysparse_parse((ysparse*)obj, filename,
                              (char*)src_, src_len,
                              dst_, dst_len);
        }
        catch (YsParseError const& exc)
        {
            throw_parse_error(env, exc.location.offset, exc.location.line, exc.location.col, exc.msg.c_str());
        }
        catch (std::exception const& exc)
        {
            throw_runtime_exception(env, exc.what());
        }
    }
    {
        TIMED_SECTION("jni:ysparse/release");
        // __S__L__O__W__
        {
            TIMED_SECTION("jni:ysparse/ReleaseByteArray(src)");
            env->ReleaseByteArrayElements(src, src_, 0);
        }
        {
            TIMED_SECTION("jni:ysparse/ReleaseIntArray(dst)");
            env->ReleaseIntArrayElements(dst, dst_, 0);
        }
        {
            TIMED_SECTION("jni:ysparse/ReleaseStringUTFChars()");
            env->ReleaseStringUTFChars(jfilename, filename);
        }
    }
    return rc;
}


JNIEXPORT jint JNICALL
Java_org_rapidyaml_Rapidyaml_ysparse_1parse_1buf(JNIEnv *env, jobject,
                                                jlong obj, jstring jfilename,
                                                jobject src, jint src_len,
                                                jobject dst, jint dst_len)
{
    TIMED_SECTION("jni:ysparse_buf", (size_type)src_len);
    char* src_ = nullptr;
    int* dst_ = nullptr;
    const char *filename = nullptr;
    {
        TIMED_SECTION("jni:ysparse_buf/get_jni", (size_type)src_len);
        src_ = (char*)env->GetDirectBufferAddress(src);
        dst_ = (int*)env->GetDirectBufferAddress(dst);
        filename = env->GetStringUTFChars(jfilename, 0);
        if(!src_)
            throw_runtime_exception(env, "null pointer: src");
        if(!dst_)
            throw_runtime_exception(env, "null pointer: dst");
    }
    {
        TIMED_SECTION("jni:ysparse_buf/parse", (size_type)src_len);
        try
        {
            return ysparse_parse((ysparse*)obj, filename, src_, src_len, dst_, dst_len);
        }
        catch (YsParseError const& exc)
        {
            throw_parse_error(env, exc.location.offset, exc.location.line, exc.location.col, exc.msg.c_str());
        }
        catch (std::exception const& exc)
        {
            throw_runtime_exception(env, exc.what());
        }
    }
    return 0; // this is executed even if there is an exception
}


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

static bool s_timing_enabled = false;
RYML_EXPORT bool ysparse_timing_get()
{
    return s_timing_enabled;
}
RYML_EXPORT void ysparse_timing_set(bool yes)
{
    s_timing_enabled = yes;
}

static C4_NO_INLINE void throw_java_exception(JNIEnv * env, const char* type, const char* msg)
{
    jclass clazz = env->FindClass(type);
    if (clazz != NULL) // if it is null, a NoClassDefFoundError was already thrown
        env->ThrowNew(clazz, msg);
}

static C4_NO_INLINE void throw_runtime_exception(JNIEnv *env, const char* msg)
{
    throw_java_exception(env, "java/lang/RuntimeException", msg);
}

static C4_NO_INLINE void throw_parse_error(JNIEnv *env, size_t offset, size_t line, size_t column, const char *msg)
{
    // see https://stackoverflow.com/questions/55013243/jni-custom-exceptions-with-more-than-one-parameter
    jclass clazz = env->FindClass("org/rapidyaml/YamlParseErrorException");
    if (clazz != NULL) // if it is null, a NoClassDefFoundError was already thrown
    {
        jstring jmsg = env->NewStringUTF(msg);
        jint joffset = (jint)offset;
        jint jline = (jint)line;
        jint jcol = (jint)column;
        // see https://www.rgagnon.com/javadetails/java-0286.html
        // about the proper signature.
        // we want <init>(int, int, int, String):
        const char * const signature = "(IIILjava/lang/String;)V";
        jmethodID ctor = env->GetMethodID(clazz, "<init>", signature);
        jobject jexc = env->NewObject(clazz, ctor, joffset, jline, jcol, jmsg);
        env->Throw((jthrowable)jexc); // https://stackoverflow.com/questions/2455668/jni-cast-between-jobect-and-jthrowable
    }
}

#ifdef __cplusplus
}
#endif
