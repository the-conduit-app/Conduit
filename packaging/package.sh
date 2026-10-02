#!/bin/sh -ex

if [ ! -f package.sh ]; then
    echo "This script must be run from the packaging directory"
    exit 1
fi

# Build the native libs. This may involve enabling Xcode.
cd ../cpp/libConduit
make install
cd ../macwindow
make install

# Back to the Conduit directory
cd ../..

rm -rf build/compose/binaries/main/app/Conduit.app
rm -f Conduit.dmg

# Build the release bundle.
# copyNativeLibsToApp depends on the release bundle Conduit.app
# being made first.
./gradlew --rerun-tasks copyNativeLibsToApp

# Build the DMG. Icons are defined in the Python script.
cd packaging
dmgbuild -s dmg_settings.py "Conduit" ../Conduit.dmg
