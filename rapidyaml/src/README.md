## Notes on JNI vs JNA
, packaging, loading, etc

From a [thread on slack](https://app.slack.com/client/T03RZGPFR/activity)

  > JNI is almost always the fasted ffi option. I've heard good things about JNR performance. JNA is usually one of the slower ffi options, but can be sped up with direct mapping, https://github.com/java-native-access/jna/blob/master/www/DirectMapping.md.


## Notes on how to profile

From a [thread on slack](https://app.slack.com/client/T03RZGPFR/activity)

  > Another tool to try to find the bottleneck is https://github.com/clojure-goes-fast/clj-async-profiler. The flamegraph might show some obvious performance issue (assuming the issue is on the jvm side).

  > Depending on how long a parse takes, I would recommend something like https://github.com/hugoduncan/criterium. time is not a good way to benchmark code unless it's a very slow function call. I would use the profiler to try and figure out where the bottleneck is.


## JNI examples

- [full JNI example](https://github.com/mkowsiak/jnicookbook/tree/master/recipes/recipeNo031)
- [full JNI example with other non-JNI shared libraries](https://github.com/mkowsiak/jnicookbook/tree/master/recipes/recipeNo035)
- [another example linking with more libraries](https://www.dynamsoft.com/codepool/package-jni-shared-library-jar-file.html)

- https://stackoverflow.com/questions/1611357/how-to-make-a-jar-file-that-includes-dll-files#comment1483970_1611367

## Notes on JNI - how to call c++ code from java

From a [thread on slack](https://app.slack.com/client/T03RZGPFR/activity)

  > I'm surprised trying to pass a mutable byte buffer doesn't cause more issues. I think the recommended way to pass a byte buffer to native is with http://java-native-access.github.io/jna/5.13.0/javadoc/com/sun/jna/Memory.html and you can get the string with .getString.
