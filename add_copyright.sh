#!/bin/bash

# Script to add GPL copyright header to Java files
# Usage: ./add_copyright.sh

COPYRIGHT_FILE="copyright_header.txt"
TEMP_FILE="temp.java"

# Check if copyright file exists
if [ ! -f "$COPYRIGHT_FILE" ]; then
    echo "Error: $COPYRIGHT_FILE not found"
    exit 1
fi

# Process all Java files
find ./src -name "*.java" | while read java_file; do
    echo "Processing: $java_file"

    # Check if file already has copyright notice
    if grep -q "Copyright" "$java_file"; then
        echo "  Already has copyright notice, skipping"
        continue
    fi

    # Create temporary file with copyright header
    cat "$COPYRIGHT_FILE" > "$TEMP_FILE"
    echo "" >> "$TEMP_FILE"  # Add blank line after header

    # Append the original file
    cat "$java_file" >> "$TEMP_FILE"

    # Replace original file
    mv "$TEMP_FILE" "$java_file"

    echo "  Added copyright header"
done

echo "Done."