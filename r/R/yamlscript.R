# Copyright 2023-2026 Ingy dot Net
# This code is licensed under MIT license (See License for details)

# R binding/API for the libys shared library.
#
# This package is an R port of the Python 'yamlscript' module, which
# is the reference implementation for YAMLScript FFI bindings to
# libys.
#
# The current user facing API consists of a single function,
# yamlscript_load(), which takes a YAMLScript string as input and
# returns the R object that the YAMLScript code evaluates to.

# This value is automatically updated by 'make bump':
YAMLSCRIPT_VERSION <- "0.2.29"

# Compile and eval a YAMLScript string and return the result:
yamlscript_load <- function(input) {
  # Call 'load_ys_to_json' in libys via the C shim:
  json <- .Call(C_yamlscript_load, as.character(input))

  # Decode the JSON response:
  resp <- jsonlite::fromJSON(json, simplifyVector = TRUE)

  # Check for libys error in JSON response:
  if (!is.null(resp$error)) {
    stop(resp$error$cause)
  }

  # Get the response object from evaluating the YAMLScript string:
  if (!("data" %in% names(resp))) {
    stop("Unexpected response from 'libys'")
  }

  resp$data
}
