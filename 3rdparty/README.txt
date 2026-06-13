This directory is for the third-party tools needed to package the RomRaider
installer distribution.

These tools are only required for the "ant all" / "ant installer" targets that
produce native installers. Compiling RomRaider ("ant build") and building the
standalone ZIP packages ("ant standalone") do NOT need them.

Two tools are required to build the installer distribution of RomRaider:

IzPack - is a widely used tool for packaging applications on the Java™ platform.
         Easily make installers that work seamlessly on Microsoft Windows™, Linux™,
		 Solaris™ and Mac OS X™.

		 Download the standalone compiler izpack-standalone-compiler-4.3.5.jar and
		 save it in the IzPack directory, rename and remove the version part (-4.3.5) of the filename.
		 https://repo1.maven.org/maven2/org/codehaus/izpack/izpack-standalone-compiler/4.3.5/izpack-standalone-compiler-4.3.5.jar

launch4j - is a cross-platform tool for wrapping Java applications distributed as jars
           in lightweight Windows native executable.

		   Download the ZIP package and extract it into the 3rdparty directory making
		   sure that launch4j.jar is in the first directory level of launch4j.
		   launch4j-3.13-win32.zip
		   https://sourceforge.net/projects/launch4j/files/launch4j-3/3.13/launch4j-3.13-win32.zip/download

Expected directory structure:
RomRaider\3rdparty\
			IzPack\
				izpack-standalone-compiler.jar
			launch4j\
				launch4j.jar
				... and other files\folders
