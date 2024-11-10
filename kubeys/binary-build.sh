#!/bin/bash

(
  read line && echo "$line"
  read line && echo "$line"
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
  sleep 1
  read line && echo "$line"
) < binary-build.txt
