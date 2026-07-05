# Install test dependencies into the local library if missing:
if (!requireNamespace("jsonlite", quietly = TRUE)) {
  install.packages(
    "jsonlite",
    lib = ".rlib",
    repos = "https://cloud.r-project.org"
  )
}
