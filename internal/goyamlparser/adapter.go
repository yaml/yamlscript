package goyamlparser

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/glojurelang/glojure/pkg/lang"
	"github.com/glojurelang/glojure/pkg/pkgmap"
)

// StartCompileProgress returns a completion callback that stops its ticker.
func StartCompileProgress(pending, success, failure string) func(bool) {
	started := time.Now()
	info, err := os.Stderr.Stat()
	terminal := err == nil && info.Mode()&os.ModeCharDevice != 0 &&
		os.Getenv("TERM") != "dumb"
	_, noColor := os.LookupEnv("NO_COLOR")
	fmt.Fprint(os.Stderr, "… ", pending)
	done, stopped := make(chan struct{}), make(chan struct{})
	if terminal {
		go func() {
			defer close(stopped)
			ticker := time.NewTicker(time.Second)
			defer ticker.Stop()
			for {
				select {
				case <-ticker.C:
					fmt.Fprint(os.Stderr, ".")
				case <-done:
					return
				}
			}
		}()
	} else {
		fmt.Fprintln(os.Stderr)
		close(stopped)
	}
	return func(ok bool) {
		close(done)
		<-stopped
		marker, message, color := "X", failure, "\x1b[31m"
		if ok {
			marker, color = "√", "\x1b[32m"
			message = fmt.Sprintf("%s (%.1fs)", success, time.Since(started).Seconds())
		}
		if terminal {
			fmt.Fprint(os.Stderr, "\r\x1b[2K")
			if !noColor {
				marker = color + marker + "\x1b[0m"
			}
		}
		fmt.Fprintln(os.Stderr, marker, message)
	}
}

func init() {
	pkgmap.Set(
		"github.com/glojurelang/glojure/pkg/lang.NewLazySeq",
		lang.NewLazySeq,
	)
}

func WriteTextFile(path, text string, mode int64) error {
	return os.WriteFile(path, []byte(text), os.FileMode(mode))
}

// WriteNewTextFile writes compiled source without replacing an existing path.
func WriteNewTextFile(path, text string, mode int64) error {
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
		return err
	}
	f, err := os.OpenFile(path, os.O_WRONLY|os.O_CREATE|os.O_EXCL, os.FileMode(mode))
	if err != nil {
		return err
	}
	_, writeErr := io.WriteString(f, text)
	closeErr := f.Close()
	if writeErr != nil || closeErr != nil {
		os.Remove(path)
		if writeErr != nil {
			return writeErr
		}
		return closeErr
	}
	return nil
}

var (
	kwEvent  = lang.NewKeyword("event")
	kwStart  = lang.NewKeyword("start")
	kwEnd    = lang.NewKeyword("end")
	kwAnchor = lang.NewKeyword("anchor")
	kwTag    = lang.NewKeyword("tag")
	kwFlow   = lang.NewKeyword("flow")
	kwValue  = lang.NewKeyword("value")
	kwStyle  = lang.NewKeyword("style")
	kwName   = lang.NewKeyword("name")
)

func markVector(mark Mark) any {
	return lang.NewVector(
		int64(mark.Line),
		int64(mark.Column),
		int64(mark.Index),
	)
}

func eventValues(name string, event *Event, capacity int) []any {
	values := make([]any, 0, capacity)
	return append(
		values,
		kwEvent,
		name,
		kwStart,
		markVector(event.StartMark),
		kwEnd,
		markVector(event.EndMark),
	)
}

func ParseYAMLScriptEvents(input string) (any, error) {
	if input == "" {
		input = "\n"
	} else if !strings.HasSuffix(input, "\n") {
		input += "\n"
	}

	parser := NewParser()
	parser.SetInputString([]byte(input))
	defer parser.Delete()

	events := make([]any, 0, len(input)/8+8)
	for {
		var event Event
		err := parser.Parse(&event)
		if err == io.EOF {
			break
		}
		if err != nil {
			return nil, err
		}
		if item := yamlScriptEvent(&event); item != nil {
			events = append(events, item)
		}
		if event.Type == STREAM_END_EVENT {
			break
		}
	}

	return lang.NewVector(events...), nil
}

func yamlScriptEvent(event *Event) any {
	var name string
	switch event.Type {
	case DOCUMENT_START_EVENT:
		name = "document_start"
	case DOCUMENT_END_EVENT:
		name = "document_end"
	case MAPPING_START_EVENT:
		return collectionEvent(
			"mapping_start",
			event,
			event.MappingStyle() == FLOW_MAPPING_STYLE,
		)
	case MAPPING_END_EVENT:
		name = "mapping_end"
	case SEQUENCE_START_EVENT:
		return collectionEvent(
			"sequence_start",
			event,
			event.SequenceStyle() == FLOW_SEQUENCE_STYLE,
		)
	case SEQUENCE_END_EVENT:
		name = "sequence_end"
	case SCALAR_EVENT:
		return scalarEvent(event)
	case ALIAS_EVENT:
		values := eventValues("alias", event, 8)
		values = append(values, kwName, string(event.Anchor))
		return lang.NewMap(values...)
	default:
		return nil
	}

	return lang.NewMap(eventValues(name, event, 6)...)
}

func nodeValues(name string, event *Event, capacity int) []any {
	values := eventValues(name, event, capacity)
	if len(event.Anchor) > 0 {
		values = append(values, kwAnchor, string(event.Anchor))
	}
	if len(event.Tag) > 0 {
		values = append(values, kwTag, string(event.Tag))
	}
	return values
}

func collectionEvent(name string, event *Event, flow bool) any {
	values := nodeValues(name, event, 12)
	if flow {
		values = append(values, kwFlow, true)
	}
	return lang.NewMap(values...)
}

func scalarEvent(event *Event) any {
	values := nodeValues("scalar", event, 14)
	values = append(values, kwValue, string(event.Value))
	if style := scalarStyle(event.ScalarStyle()); style != "" {
		values = append(values, kwStyle, style)
	}
	return lang.NewMap(values...)
}

func scalarStyle(style ScalarStyle) string {
	switch style {
	case SINGLE_QUOTED_SCALAR_STYLE:
		return "single"
	case DOUBLE_QUOTED_SCALAR_STYLE:
		return "double"
	case LITERAL_SCALAR_STYLE:
		return "literal"
	case FOLDED_SCALAR_STYLE:
		return "folded"
	default:
		return ""
	}
}
