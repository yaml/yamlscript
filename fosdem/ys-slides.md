## YAML as a Programming Language

* Many people try to do programming things with YAML
  * But YAML wasn't designed for that
* Ingy has been working on a new YAML based programming language
* Complete and general-purpose
* Best when embedded in plain old YAML files
  * Load data from other files
  * Excellent interpolation features
  * Merge, filter, concatenate, etc. Any functions you can imagine
  * Define your own functions
* Solves most programming things that people want to do with YAML

----

## YAMLScript

* https://yamlscript.org

* YAMLScript is a new functional programming language
* Syntax is valid YAML
* Clean, expressive, mixes well with YAML data
* Execution speed is on par with Python or Ruby
* YAMLScript loader modules for all major programming languages
  * Perl, Python, Raku, Ruby and Rust so far
* Your YAML files are already valid YAMLScript programs
* Load them as normal YAML 1.2 data files
* Get YS function powers by adding `!yamlscript/v0` to the top


----

<table>
<tr>

<td><b>YAML</b></td>
<td><b>YAMLScript</b></td>
<td><b>Run It!</b></td>
</tr>
<tr>
<td><code>some.yaml</code></td>
<td><code>generator.ys</code></td>
<td><code>$ ys --load generator.ys | jq</code></td>
</tr>
<tr>
<td>

```yaml
people:
- Ingy
- Tina
- Panto
- Eemeli
- Thom

places:
- Seattle
- Berlin
- Athens
- Helsinki
- Buffalo
```
</td>
<td>

```yaml
!yamlscript/v0

data =: load("some.yaml")
people =: data.people
places =: data.places

defn other(max, person, place)::
  string:: "$person likes $place."
  number:: int(rand(max)) + 1

take (ARGV.0 || 5):
  shuffle:
    for [person people, place places]:
      merge:
        =>::
          person:: person
          place::  place
```
</td>
<td>

```json
[
  {
    "person": "Thom",
    "place": "Seattle",
    "string": "Thom likes Seattle.",
    "number": 4
  },
  {
    "person": "Eemeli",
    "place": "Berlin",
    "string": "Eemeli likes Berlin.",
    "number": 6
  }
]
```
</td>
</tr>
</table>

----

## Try YAMLScript today!

* Run: `. <(curl -sL yamlscript.org/try-ys)`

* Installs `ys` under `/tmp` and adds it to your `PATH`

* For current shell (Bash or Zsh) session only

```txt
$ . <(curl -sL yamlscript.org/try-ys)
Installed /tmp/yamlscript-try-ys-thwJgNAUG0/bin/ys - version 0.1.36

Try out these commands:

    ys --help

    ys -e 'each [i (1 .. 5)]: say("Hello!")'

    ys -e 'each [i (1 .. 5)]:' -e '  say: "Hello!"'
    ys -e 'each [i (1 .. 5)]:' -e '  say: "Hello!"' --compile
    ys -e 'each [i (1 .. 5)]:' -e '  say: "Hello!"' --load

    curl -sL yamlscript.org/try/99-bottles.ys | ys - 3
    curl -sL yamlscript.org/try/99-bottles.ys | ys - -c -X
```
