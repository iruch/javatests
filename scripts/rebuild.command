#!/usr/bin/env bash
# macOS: double-click in Finder opens Terminal and runs the full rebuild, then closes the window.
exec "$(dirname "$0")/rebuild.sh" --close-terminal "$@"
