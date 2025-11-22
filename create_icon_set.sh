#!/bin/bash
# Script to create a complete icon set from a base PNG icon
# Usage: ./create_icon_set.sh <icon_name> <base_icon.png> [output_dir]

set -e

if [ $# -lt 2 ]; then
    echo "Usage: $0 <icon_name> <base_icon.png> [output_dir]"
    echo ""
    echo "Example: $0 myicon myicon_base.png"
    echo "  Creates: home_btn_myicon_default.png, _pressed.png, _selected.png"
    echo "           and home_btn_myicon.xml"
    echo ""
    echo "Options:"
    echo "  icon_name     - Name for the icon (without 'home_btn_' prefix)"
    echo "  base_icon.png - Path to your base PNG icon"
    echo "  output_dir    - Optional: output directory (default: current directory)"
    exit 1
fi

ICON_NAME="$1"
BASE_ICON="$2"
OUTPUT_DIR="${3:-.}"

# Check if ImageMagick is available
if ! command -v convert &> /dev/null; then
    echo "Error: ImageMagick 'convert' command not found."
    echo "Install it with: brew install imagemagick (macOS) or apt-get install imagemagick (Linux)"
    exit 1
fi

# Check if base icon exists
if [ ! -f "$BASE_ICON" ]; then
    echo "Error: Base icon file not found: $BASE_ICON"
    exit 1
fi

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Generate icon variants
echo "Creating icon variants from $BASE_ICON..."

# Default (copy as-is)
DEFAULT="${OUTPUT_DIR}/home_btn_${ICON_NAME}_default.png"
cp "$BASE_ICON" "$DEFAULT"
echo "  Created: $DEFAULT"

# Pressed (darken by 25%)
PRESSED="${OUTPUT_DIR}/home_btn_${ICON_NAME}_pressed.png"
convert "$BASE_ICON" -brightness-contrast -25x0 "$PRESSED"
echo "  Created: $PRESSED"

# Selected (brighten by 15%)
SELECTED="${OUTPUT_DIR}/home_btn_${ICON_NAME}_selected.png"
convert "$BASE_ICON" -brightness-contrast 15x0 "$SELECTED"
echo "  Created: $SELECTED"

# Create the selector XML file
XML_FILE="${OUTPUT_DIR}/home_btn_${ICON_NAME}.xml"
cat > "$XML_FILE" << EOF
<!--
  Copyright 2011 Google Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@drawable/home_btn_${ICON_NAME}_pressed"
        android:state_focused="true"
        android:state_pressed="true" />
    <item android:drawable="@drawable/home_btn_${ICON_NAME}_pressed"
        android:state_focused="false"
        android:state_pressed="true" />
    <item android:drawable="@drawable/home_btn_${ICON_NAME}_selected" android:state_focused="true" />
    <item android:drawable="@drawable/home_btn_${ICON_NAME}_default"
        android:state_focused="false"
        android:state_pressed="false" />
</selector>
EOF

echo "  Created: $XML_FILE"
echo ""
echo "Done! Created icon set for 'home_btn_${ICON_NAME}'"
echo ""
echo "Next steps:"
echo "1. Copy PNG files to: app/src/main/res/drawable-hdpi/"
echo "2. Copy XML file to: app/src/main/res/drawable/"
echo "3. Add button to fragment_dashboard.xml"
echo "4. Add click handler in DashboardFragment.java"

