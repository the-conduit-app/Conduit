#!/bin/sh -ex

cd ..

rm -rf build/compose/binaries/main/app/Conduit.app
rm -f Conduit.dmg

# copyNativeLibsToApps depends on making the release bundle Conduit.app,
# after that's made we build the dmg. Icons are defined in the python
# script

./gradlew --rerun-tasks copyNativeLibsToApp

cd packaging

dmgbuild -s dmg_settings.py "Conduit" ../Conduit.dmg
