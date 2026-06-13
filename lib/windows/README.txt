This directory holds Windows-specific native libraries placed on the
java.library.path at runtime (-Djava.library.path=lib/windows).

As of the 2026 modernization it is normally empty: the native code for JNA,
com4j, hid4java and jSerialComm is bundled inside those jars and extracted
automatically, and Windows J2534 pass-thru drivers are discovered from the
registry / vendor installation rather than shipped here.

The directory is kept so the Ant build and the distribution packaging, which
reference lib/windows, continue to work. Drop any optional vendor native
(.dll) libraries here if you need them on the library path.
