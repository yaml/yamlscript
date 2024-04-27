# graal-native-image-jni

### Aim

Try and build the smallest possible JNI example to test GraalVM's native-image
JNI support.

### Result

Success.

```
$ ./helloworld
Hello world; this is C talking!
```

### Insight

In order for native-image to successfuly load a c library to execute, it must
run the `System.loadLibrary()` call at runtime, not at build time.

### Method 1: Put loadLibrary in the execution path

This is the version we have done.
By putting loadLibrary inside the `main` method, the library is loaded at run
time.
With this setup we can compile with `--initialize-at-build-time` and everything
will work.

### Method 2: Put loadLibrary in static class initializer and use --initialize-at-run-time

Sometimes you don't have control over where you call loadLibrary from.
Often existing code places it in the slasses static initializer block.
In this case the library is loaded at build time, but then when the final
artifact is run, the linked code cannot be found and the programme crashes with
a `java.lang.UnsatisfiedLinkError` exception.

When you place the loadLibrary call within a static block of a class, you must
specify to `native-image` that your class should be initialized at runtime.
