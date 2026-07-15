## Kotlin Usage

Use `kotlin-yamlscript` as a drop-in replacement for your current
YAML loader:

File `main.kt`:

```kotlin
import java.io.File
import org.yamlscript.yamlscript.YS

fun main() {
    val data = YS.loadObject(File("config.yaml").readText())
    println(data.toString(2))
}
```


## Installation

Add the `kotlin-yamlscript` artifact to your project and install the
`libys.so` shared library:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.yamlscript:kotlin-yamlscript:0.2.27")
}
```

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* JDK 8 or higher
* Linux, macOS or Windows
