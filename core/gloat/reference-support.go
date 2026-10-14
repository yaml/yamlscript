// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package goyamlparser

import "github.com/glojurelang/glojure/pkg/lang"

// Reference views implement the ordinary persistent collection interfaces.
// Looking at keys or a collection's size does not evaluate its children.
type referenceMap struct {
	lang.IPersistentMap
	lookup lang.IFn
	meta   lang.IPersistentMap
}

func ReferenceMap(source lang.IPersistentMap, lookup lang.IFn,
	meta lang.IPersistentMap) lang.IPersistentMap {
	return &referenceMap{source, lookup, meta}
}

func (m *referenceMap) Meta() lang.IPersistentMap { return m.meta }
func (m *referenceMap) WithMeta(meta lang.IPersistentMap) any {
	return ReferenceMap(m.IPersistentMap, m.lookup, meta)
}
func (m *referenceMap) ValAt(key any) any { return m.ValAtDefault(key, nil) }
func (m *referenceMap) ValAtDefault(key, missing any) any {
	if !m.ContainsKey(key) {
		return missing
	}
	return m.lookup.Invoke(key)
}
func (m *referenceMap) EntryAt(key any) lang.IMapEntry {
	if !m.ContainsKey(key) {
		return nil
	}
	return &referenceEntry{lang.NewMapEntry(key, nil), m.lookup}
}
func (m *referenceMap) Seq() lang.ISeq {
	var entries func(lang.ISeq) lang.ISeq
	entries = func(source lang.ISeq) lang.ISeq {
		if source == nil {
			return nil
		}
		return lang.NewLazySeq(func() any {
			key := source.First().(lang.IMapEntry).Key()
			entry := &referenceEntry{lang.NewMapEntry(key, nil), m.lookup}
			return lang.NewCons(entry, entries(source.Next()))
		})
	}
	return entries(m.IPersistentMap.Seq())
}
func (m *referenceMap) Assoc(key, value any) lang.Associative {
	lookup := lang.NewFnFunc1(func(candidate any) any {
		if lang.Equiv(candidate, key) {
			return value
		}
		return m.lookup.Invoke(candidate)
	})
	return ReferenceMap(m.IPersistentMap.Assoc(key, value).(lang.IPersistentMap),
		lookup, nil)
}
func (m *referenceMap) AssocEx(key, value any) lang.IPersistentMap {
	if m.ContainsKey(key) {
		panic("key already present")
	}
	return m.Assoc(key, value).(lang.IPersistentMap)
}
func (m *referenceMap) Without(key any) lang.IPersistentMap {
	return ReferenceMap(m.IPersistentMap.Without(key), m.lookup, nil)
}
func (m *referenceMap) Cons(value any) lang.Conser {
	if entry, ok := value.(lang.IMapEntry); ok {
		return m.Assoc(entry.Key(), entry.Val()).(lang.Conser)
	}
	if pair, ok := value.(lang.IPersistentVector); ok && pair.Count() == 2 {
		return m.Assoc(pair.Nth(0), pair.Nth(1)).(lang.Conser)
	}
	var result lang.Conser = m
	for entries := lang.Seq(value); entries != nil; entries = entries.Next() {
		result = result.Cons(entries.First())
	}
	return result
}
func (m *referenceMap) Invoke(args ...any) any {
	if len(args) == 1 {
		return m.ValAt(args[0])
	}
	if len(args) == 2 {
		return m.ValAtDefault(args[0], args[1])
	}
	panic("map expects one or two arguments")
}
func (m *referenceMap) ApplyTo(args lang.ISeq) any {
	return m.Invoke(referenceArguments(args)...)
}
func (m *referenceMap) Equiv(other any) bool {
	if other == m {
		return true
	}
	mapValue, ok := other.(lang.IPersistentMap)
	if !ok || mapValue.Count() != m.Count() {
		return false
	}
	for entries := m.IPersistentMap.Seq(); entries != nil; entries = entries.Next() {
		key := entries.First().(lang.IMapEntry).Key()
		if !mapValue.ContainsKey(key) || !lang.Equiv(m.ValAt(key), mapValue.ValAt(key)) {
			return false
		}
	}
	return true
}
func (m *referenceMap) HashEq() uint32 {
	var values []any
	for entries := m.Seq(); entries != nil; entries = entries.Next() {
		entry := entries.First().(lang.IMapEntry)
		values = append(values, entry.Key(), entry.Val())
	}
	return lang.HashEq(lang.NewMap(values...))
}

func (m *referenceMap) KVReduce(f lang.IFn, initial any) any {
	result := initial
	for keys := ReferenceKeys(m); keys != nil; keys = keys.Next() {
		key := keys.First()
		result = f.Invoke(result, key, m.ValAt(key))
		if lang.IsReduced(result) {
			return result.(lang.IDeref).Deref()
		}
	}
	return result
}

// Glojure's stock keys/vals sequences require its concrete MapEntry type.
// Preserve lazy entries by exposing the shape directly for reference maps.
func ReferenceKeys(value any) lang.ISeq {
	for {
		view, ok := value.(*referenceMap)
		if !ok {
			return lang.Keys(value)
		}
		value = view.IPersistentMap
	}
}

func ReferenceVals(value any) lang.ISeq {
	if view, ok := value.(*referenceMap); ok {
		return lang.NewMappedSeq(view.lookup, ReferenceKeys(view))
	}
	return lang.Vals(value)
}

type referenceEntry struct {
	*lang.MapEntry
	lookup lang.IFn
}

func (e *referenceEntry) Val() any { return e.lookup.Invoke(e.Key()) }
func (e *referenceEntry) Nth(index int) any {
	if index == 0 {
		return e.Key()
	}
	if index == 1 {
		return e.Val()
	}
	panic(lang.NewIndexOutOfBoundsError())
}
func (e *referenceEntry) NthDefault(index int, missing any) any {
	if index < 0 || index > 1 {
		return missing
	}
	return e.Nth(index)
}
func (e *referenceEntry) Seq() lang.ISeq {
	return lang.NewVector(e.Key(), e.Val()).Seq()
}

type referenceVector struct {
	lang.IPersistentVector
	lookup lang.IFn
	meta   lang.IPersistentMap
}

func ReferenceVector(source lang.IPersistentVector, lookup lang.IFn,
	meta lang.IPersistentMap) lang.IPersistentVector {
	return &referenceVector{source, lookup, meta}
}
func (v *referenceVector) Meta() lang.IPersistentMap { return v.meta }
func (v *referenceVector) WithMeta(meta lang.IPersistentMap) any {
	return ReferenceVector(v.IPersistentVector, v.lookup, meta)
}
func (v *referenceVector) Nth(index int) any {
	if index < 0 || index >= v.Count() {
		panic(lang.NewIndexOutOfBoundsError())
	}
	return v.lookup.Invoke(int64(index))
}
func (v *referenceVector) NthDefault(index int, missing any) any {
	if index < 0 || index >= v.Count() {
		return missing
	}
	return v.Nth(index)
}
func (v *referenceVector) ValAt(key any) any { return v.ValAtDefault(key, nil) }
func (v *referenceVector) ValAtDefault(key, missing any) any {
	index, ok := lang.AsInt(key)
	if !ok {
		return missing
	}
	return v.NthDefault(index, missing)
}
func (v *referenceVector) EntryAt(key any) lang.IMapEntry {
	if !v.ContainsKey(key) {
		return nil
	}
	return lang.NewMapEntry(key, v.ValAt(key))
}
func (v *referenceVector) AssocN(index int, value any) lang.IPersistentVector {
	lookup := lang.NewFnFunc1(func(candidate any) any {
		position, _ := lang.AsInt(candidate)
		if position == index {
			return value
		}
		return v.lookup.Invoke(candidate)
	})
	return ReferenceVector(v.IPersistentVector.AssocN(index, value), lookup, nil)
}
func (v *referenceVector) Assoc(key, value any) lang.Associative {
	index, ok := lang.AsInt(key)
	if !ok {
		panic("vector index must be an integer")
	}
	return v.AssocN(index, value)
}
func (v *referenceVector) Cons(value any) lang.Conser {
	return v.AssocN(v.Count(), value)
}
func (v *referenceVector) Pop() lang.IPersistentStack {
	return ReferenceVector(v.IPersistentVector.Pop().(lang.IPersistentVector),
		v.lookup, nil)
}
func (v *referenceVector) Peek() any {
	if v.Count() == 0 {
		return nil
	}
	return v.Nth(v.Count() - 1)
}
func (v *referenceVector) Seq() lang.ISeq {
	var entries func(int) lang.ISeq
	entries = func(index int) lang.ISeq {
		if index >= v.Count() {
			return nil
		}
		return lang.NewLazySeq(func() any {
			return lang.NewCons(v.Nth(index), entries(index+1))
		})
	}
	return entries(0)
}
func (v *referenceVector) RSeq() lang.ISeq {
	values := make([]any, v.Count())
	for index := range values {
		values[index] = v.Nth(v.Count() - index - 1)
	}
	return lang.NewVector(values...).Seq()
}
func (v *referenceVector) Invoke(args ...any) any {
	if len(args) != 1 {
		panic("vector expects one argument")
	}
	index, ok := lang.AsInt(args[0])
	if !ok {
		panic("vector index must be an integer")
	}
	return v.Nth(index)
}
func (v *referenceVector) ApplyTo(args lang.ISeq) any {
	return v.Invoke(referenceArguments(args)...)
}
func (v *referenceVector) Equiv(other any) bool {
	if other == v {
		return true
	}
	if _, ok := other.(lang.Sequential); !ok {
		return false
	}
	left, right := v.Seq(), lang.Seq(other)
	for left != nil && right != nil {
		if !lang.Equiv(left.First(), right.First()) {
			return false
		}
		left, right = left.Next(), right.Next()
	}
	return left == nil && right == nil
}
func (v *referenceVector) HashEq() uint32 {
	return lang.HashEq(lang.NewVector(referenceArguments(v.Seq())...))
}

func (v *referenceVector) KVReduce(f lang.IFn, initial any) any {
	result := initial
	for index := 0; index < v.Count(); index++ {
		result = f.Invoke(result, int64(index), v.Nth(index))
		if lang.IsReduced(result) {
			return result.(lang.IDeref).Deref()
		}
	}
	return result
}

func referenceArguments(args lang.ISeq) []any {
	var values []any
	for ; args != nil; args = args.Next() {
		values = append(values, args.First())
	}
	return values
}

func ReferenceQueue() chan any                { return make(chan any, 1) }
func ReferenceSend(queue chan any, value any) { queue <- value }
func ReferenceReceive(queue chan any) any     { return <-queue }
func ReferenceStart(f lang.IFn) {
	frame := lang.CloneThreadBindingFrame()
	go func() {
		lang.ResetThreadBindingFrame(frame)
		f.Invoke()
	}()
}
