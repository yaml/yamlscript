import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

class HelloWorld {
    // putting System.loadLibrary() here forces us
    // to mark this class to initialize at runtime
    // when building with native-image
    // https://github.com/oracle/graal/issues/1828
    //
    // static {
    //     System.loadLibrary("HelloWorld");
    // }

    private native void print();
    private native void print_num_plus_2(int num);

    private native int foo(byte[] src, int src_len, byte[] dest, int dest_len);

    // entry point
    public static void main(String[] args) {
        // instead we System.loadLibrary() inside the execution path
        // to load the library file
        System.loadLibrary("helloworld");

        new HelloWorld().print();
        new HelloWorld().print_num_plus_2(40);

        String str = "let's try our luck";
        byte[] src = str.getBytes(StandardCharsets.UTF_8);
        byte[] dst = new byte[100];

        int rc = new HelloWorld().foo(
            src, src.length, dst, dst.length
        );

        String result = new String(dst, StandardCharsets.UTF_8);

        System.out.println(rc);
        System.out.println(result);
    }
}


//int jni_toy_doit(const char *src, int src_len,
//                 /* */ char *dst, int dst_len)
//{
//    const char *success = "it_works!";
//    const int suc_len = (int)strlen(success);
//    const int req_len = suc_len + 2/* '*/ + src_len + 1/*'*/ + 1/*\0*/;
//    if(req_len <= dst_len)
//    {
//        int ret = snprintf(dst, (size_t)dst_len,
//                           "%s '%.*s'", success, (int)src_len, src);
//    }
//    return req_len;
//}


