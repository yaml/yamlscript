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

  > > But I was thinking whether it is possible/practical/advisable (in terms of speed) to build the Clojure dictionary directly in the C++ code. Currently the C++ code is providing an EDN markup string that is later parsed in Clojure. Assuming a large dictionary of say ~40k entries, would it be possible to call native clojure/java JNI functions to build the final structure instead of creating the intermediate EDN? Would that be a gain? Would the ~40k calls to JNI end up costing too much?
  > This isn't something I've tried before, so take it with a grain of salt, but you have at least a few options with different tradeoffs. All the collection types in clojure are based on protocols/interfaces, so it would be possible to just return a pointer, with no copying, and wrap it proxy that implements all the relevant interfaces for maps/lists/etc. When JVM code asks for a value from a map or element from list, you produce the JVM value for numbers/strings or you return another proxy pointer if it's a collection. You might still have to make a copy to return a string value. If you're returning large values that you expect will only be partially read or read only once, then lazily producing jvm values might be a win. If you expect the large value to be completely read multiple times, then it could be potentially faster to just convert the full data structure to a JVM value. There's also intermediate options where you do some of the work upfront, and do some of the work lazily. Granularity will also affect memory usage. You probably don't want some scenario where someone parses a giant blob and keeps only a small part, but still has to hold the giant value in memory until the small part gets reclaimed.I don't have a good answer for you here. My intuition is that your approach of building the final data structure in c++ is probably a good idea, but I don't really have the experience to say for sure.This type of question might get a better answer in #data-science. I think they similar issues with dealing with large datasets that are partially processed in native code. They've also built deep integrations with python via https://github.com/clj-python/libpython-clj where I think they've run into similar problems.
