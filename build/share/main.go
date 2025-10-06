package main

import (
	"os"
	"github.com/glojurelang/glojure/pkg/glj"
	"github.com/glojurelang/glojure/pkg/lang"
	_ "build.yamlscript.org/GLJ-MAIN-NAMESPACE/GO-MAIN-PACKAGE"
	_ "build.yamlscript.org/GLJ-MAIN-NAMESPACE/ys/v0"
)

func main() {
	require := glj.Var("clojure.core", "require")
	require.Invoke(lang.NewSymbol("ys.v0")) //
	require.Invoke(lang.NewSymbol("GLJ-MAIN-NAMESPACE"))
	myMain := glj.Var("GLJ-MAIN-NAMESPACE", "-main")
	args := os.Args[1:]
	anyArgs := make([]any, len(args))
	for i, arg := range args {
		anyArgs[i] = arg
	}
	myMain.Invoke(anyArgs...)
}
