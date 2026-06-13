#!/bin/bash

# To change your ZIP install startup preference, uncomment the preferred line.
# Make sure only one line is uncommented
# Java is expected to be available on your PATH (Java 17 or newer)

# Select the native library path that matches the running JVM (64-bit by
# default, falling back to 32-bit on legacy systems).
ARCH_BITS=$(java -XshowSettings:properties -version 2>&1 | grep -m1 'sun.arch.data.model' | tr -dc '0-9')
case "$ARCH_BITS" in
    32) NATIVE_PATH=lib/linux/32 ;;
    *)  NATIVE_PATH=lib/linux/64 ;;
esac

mkdir -p "$HOME/.RomRaider"

JVM_OPTS="-Djava.library.path=${NATIVE_PATH} -Dawt.useSystemAAFontSettings=lcd -Dswing.aatext=true -Dsun.java2d.d3d=false -Xms64M -Xmx512M -XX:CompileThreshold=10000"

java $JVM_OPTS -jar RomRaider.jar >> "$HOME/.RomRaider/romraider_sout.log" 2>&1
#java $JVM_OPTS -jar RomRaider.jar -logger >> "$HOME/.RomRaider/romraider_sout.log" 2>&1
#java $JVM_OPTS -jar RomRaider.jar -logger.fullscreen >> "$HOME/.RomRaider/romraider_sout.log" 2>&1
#java $JVM_OPTS -jar RomRaider.jar -logger.touch >> "$HOME/.RomRaider/romraider_sout.log" 2>&1

exit 0
