package goyamlparser

import "fmt"

type Style uint32

type DepthKind string

const (
	DepthKindFlow  DepthKind = "flow"
	DepthKindBlock DepthKind = "block"
)

type DepthContext struct {
	Kind DepthKind
}

type IndentMode string

const (
	IndentModeAuto IndentMode = "auto"
	IndentModeTabs IndentMode = "tabs"
)

type IndentStyle string

const (
	IndentStyleAuto   IndentStyle = "auto"
	IndentStyleSpaces IndentStyle = "spaces"
	IndentStyleTabs   IndentStyle = "tabs"
)

type IndentScope string

const (
	IndentScopeDocument IndentScope = "document"
	IndentScopeStream   IndentScope = "stream"
)

type IndentConfig struct {
	Mode      IndentMode
	LoadStyle IndentStyle
	DumpStyle IndentStyle
	Scope     IndentScope
}

func DefaultDepthCheck(depth int, ctx *DepthContext) error {
	const maxDepth = 10000
	if depth > maxDepth {
		return fmt.Errorf("exceeded max depth of %d", maxDepth)
	}
	return nil
}

type Composer struct {
	Parser Parser
}

func NewComposer(in []byte, options any) *Composer {
	parser := NewParser()
	parser.SetInputString(in)
	return &Composer{Parser: parser}
}

func (c *Composer) Destroy() {}

func (e *Event) Delete() {}
