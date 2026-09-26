"""Keep MkDocs from probing system MIME files in a sandbox."""

import mimetypes


mimetypes.knownfiles = []
