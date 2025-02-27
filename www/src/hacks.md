---
title: YS Hacks
talk: 0
---


Here are some interesting real-world hacks that have been done with YS.

## YAMLLM

YAMLLM is a command line tool [written in YS](
https://github.com/yaml/yamllm/blob/main/bin/yamllm.ys) to query common LLM
APIs.

It currently works with these APIs from [OpenAI](https://openai.com/),
[Anthropic](https://anthropic.com/), and [Groq](https://groq.com/):

  * Anthropic Models
    - claude-3-5-haiku-latest
    - claude-3-5-sonnet-latest
    - claude-3-opus-latest

  * Groq Models
    - llama3-70b-8192
    - llama3-8b-8192
    - gemma-7b-it
    - gemma2-9b-it
    - mixtral-8x7b-32768
    - whisper-large-v3

  * Openai Models
    - gpt-4o
    - gpt-4o-mini
    - gpt-4-turbo
    - gpt-4
    - gpt-3.5-turbo
    - dall-e-2
    - dall-e-3
