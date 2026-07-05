# Copyright 2023-2026 Ingy dot Net
# This code is licensed under MIT license (See License for details)

# Test the yamlscript R binding.
# Run with: Rscript tests/test-yamlscript.R

library(yamlscript)

fails <- 0

check <- function(cond, label) {
  if (isTRUE(cond)) {
    cat("ok -", label, "\n")
  } else {
    cat("not ok -", label, "\n")
    fails <<- fails + 1
  }
}

# Load YS code:
data <- yamlscript_load("!ys-0:\ntest:: inc(41)")
check(data$test == 42, "load ys code")

# Load plain YAML:
data <- yamlscript_load("foo: bar")
check(data$foo == "bar", "load plain yaml")

# Load invalid input raises:
threw <- tryCatch(
  {
    yamlscript_load(":")
    FALSE
  },
  error = function(e) TRUE
)
check(threw, "load error raises")

# Load multiple times:
data <- yamlscript_load("!ys-0:\ntest:: inc(41)")
check(data$test == 42, "load multiple times")

if (fails > 0) {
  cat(fails, "test(s) failed\n")
  quit(status = 1)
}
