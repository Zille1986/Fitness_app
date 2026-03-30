#!/bin/bash
#
# GoSteady iOS + watchOS Project Setup
#
# This script installs XcodeGen (if needed) and generates the .xcodeproj
# from project.yml. Run this once after cloning, or any time you change
# project.yml, add/remove source files, or modify targets.
#
# Usage:
#   cd /Users/daniel/Fitness_app/iosApp
#   ./setup_project.sh
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== GoSteady Project Setup ==="
echo ""

# ── Step 1: Ensure XcodeGen is installed ──────────────────────────────

if command -v xcodegen &>/dev/null; then
    echo "[OK] XcodeGen is installed: $(xcodegen --version)"
else
    echo "[INFO] XcodeGen not found. Installing via Homebrew..."
    if command -v brew &>/dev/null; then
        brew install xcodegen
        echo "[OK] XcodeGen installed."
    else
        echo "[ERROR] Homebrew is not installed. Install XcodeGen manually:"
        echo "  brew install xcodegen"
        echo "  OR: mint install yonaskolb/XcodeGen"
        exit 1
    fi
fi

# ── Step 2: Ensure directory structure exists ─────────────────────────

echo ""
echo "Ensuring directory structure..."

mkdir -p GoSteady/Sources/App
mkdir -p GoSteady/Sources/Models
mkdir -p GoSteady/Sources/ViewModels
mkdir -p GoSteady/Sources/Screens
mkdir -p GoSteady/Sources/Components
mkdir -p GoSteady/Sources/Services
mkdir -p GoSteady/Sources/Utils
mkdir -p GoSteady/Sources/Theme
mkdir -p GoSteady/Assets.xcassets/AppIcon.appiconset
mkdir -p GoSteady/Assets.xcassets/AccentColor.colorset
mkdir -p GoSteady/Assets.xcassets/LaunchBackground.colorset
mkdir -p GoSteady/Tests
mkdir -p GoSteady/UITests
mkdir -p GoSteady/Configs

mkdir -p ../watchOSApp/GoSteadyWatch/Sources/App
mkdir -p ../watchOSApp/GoSteadyWatch/Sources/Models
mkdir -p ../watchOSApp/GoSteadyWatch/Sources/Screens
mkdir -p ../watchOSApp/GoSteadyWatch/Sources/Services
mkdir -p ../watchOSApp/GoSteadyWatch/Sources/Theme
mkdir -p ../watchOSApp/GoSteadyWatch/Assets.xcassets/AppIcon.appiconset
mkdir -p ../watchOSApp/GoSteadyWatch/Assets.xcassets/AccentColor.colorset

echo "[OK] Directories created."

# ── Step 3: Create asset catalog stubs if missing ─────────────────────

if [ ! -f "GoSteady/Assets.xcassets/Contents.json" ]; then
    cat > GoSteady/Assets.xcassets/Contents.json <<'JSON'
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
fi

if [ ! -f "GoSteady/Assets.xcassets/AccentColor.colorset/Contents.json" ]; then
    cat > GoSteady/Assets.xcassets/AccentColor.colorset/Contents.json <<'JSON'
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "0.349",
          "green" : "0.820",
          "red" : "0.290"
        }
      },
      "idiom" : "universal"
    },
    {
      "appearances" : [
        {
          "appearance" : "luminosity",
          "value" : "dark"
        }
      ],
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "0.400",
          "green" : "0.900",
          "red" : "0.350"
        }
      },
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
fi

if [ ! -f "GoSteady/Assets.xcassets/LaunchBackground.colorset/Contents.json" ]; then
    cat > GoSteady/Assets.xcassets/LaunchBackground.colorset/Contents.json <<'JSON'
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "1.000",
          "green" : "1.000",
          "red" : "1.000"
        }
      },
      "idiom" : "universal"
    },
    {
      "appearances" : [
        {
          "appearance" : "luminosity",
          "value" : "dark"
        }
      ],
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "0.078",
          "green" : "0.078",
          "red" : "0.078"
        }
      },
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
fi

if [ ! -f "GoSteady/Assets.xcassets/AppIcon.appiconset/Contents.json" ]; then
    cat > GoSteady/Assets.xcassets/AppIcon.appiconset/Contents.json <<'JSON'
{
  "images" : [
    {
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
fi

# Watch asset catalogs
if [ ! -f "../watchOSApp/GoSteadyWatch/Assets.xcassets/Contents.json" ]; then
    cat > ../watchOSApp/GoSteadyWatch/Assets.xcassets/Contents.json <<'JSON'
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
fi

if [ ! -f "../watchOSApp/GoSteadyWatch/Assets.xcassets/AppIcon.appiconset/Contents.json" ]; then
    cat > ../watchOSApp/GoSteadyWatch/Assets.xcassets/AppIcon.appiconset/Contents.json <<'JSON'
{
  "images" : [
    {
      "idiom" : "watch",
      "platform" : "watchOS",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
fi

if [ ! -f "../watchOSApp/GoSteadyWatch/Assets.xcassets/AccentColor.colorset/Contents.json" ]; then
    cat > ../watchOSApp/GoSteadyWatch/Assets.xcassets/AccentColor.colorset/Contents.json <<'JSON'
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "0.349",
          "green" : "0.820",
          "red" : "0.290"
        }
      },
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
fi

echo "[OK] Asset catalogs ready."

# ── Step 4: Create Secrets.plist template if missing ──────────────────

if [ ! -f "GoSteady/Secrets.plist" ]; then
    cat > GoSteady/Secrets.plist <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>GEMINI_API_KEY</key>
	<string>YOUR_GEMINI_API_KEY_HERE</string>
	<key>STRAVA_CLIENT_ID</key>
	<string>YOUR_STRAVA_CLIENT_ID_HERE</string>
	<key>STRAVA_CLIENT_SECRET</key>
	<string>YOUR_STRAVA_CLIENT_SECRET_HERE</string>
	<key>MAPS_API_KEY</key>
	<string>YOUR_MAPS_API_KEY_HERE</string>
</dict>
</plist>
XML
    echo "[OK] Created Secrets.plist template. Fill in your API keys before building."
else
    echo "[OK] Secrets.plist already exists."
fi

# ── Step 5: Generate .xcodeproj ───────────────────────────────────────

echo ""
echo "Generating Xcode project from project.yml..."
xcodegen generate --spec project.yml

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "  1. Fill in API keys in GoSteady/Secrets.plist"
echo "  2. Set your DEVELOPMENT_TEAM in Xcode (Signing & Capabilities)"
echo "  3. Add app icon to GoSteady/Assets.xcassets/AppIcon.appiconset/"
echo "  4. Open GoSteady.xcodeproj and build"
echo ""
echo "To regenerate after adding/removing files:"
echo "  cd $(pwd) && xcodegen generate"
echo ""
