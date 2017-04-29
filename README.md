SikuliX Version 2 - Java API
============

**Version 2.0.0 under developement** [![Join the chat at https://gitter.im/RaiMan/SikuliX2](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/RaiMan/SikuliX2?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/RaiMan/SikuliX2.svg?branch=master)](https://travis-ci.org/RaiMan/SikuliX2)

**[Download](http://sikulix.com) latest ready to use version 1.1.1** [or get the Sources](https://github.com/RaiMan/SikuliX-2014)

<hr>

Travis CI (Ubuntu 12.04-64, Java 8, xvfb)
<br>Developing and testing locally on macOS 12 and Windows 10 with Java 8
<br>Translation project: [Transifex::SikuliX2](https://www.transifex.com/sikulix/sikulix2/dashboard/)
<br>Documentation project: [ReadTheDocs::SikuliX2](http://sikulix2.readthedocs.org/) based on [GitHub::SikuliX2-Docs](https://github.com/RaiMan/SikuliX2-Docs)

**Forking and/or downloading this repo only makes sense:**

 - if you want to get a knowledge about the internals of SikuliX
 - if you want to create your own packages containing SikuliX features
 - if you want to contribute.

<hr>

**BE AWARE** For use with Java aware scripting and Java programming you might need additional stuff and steps at your own resposibility. Currently you might also fork the project and make your tests in the project context. Issues and pull requests are only accepted here on Github.

<hr>

**sikulixapi**

the API to be used in Java and Java aware scripting languages

Snapshots available on OSSRH (should work in Maven and Gradle projects):

```
  <repositories>
    <repository>
      <id>snapshot</id>
      <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>com.sikulix</groupId>
      <artifactId>sikulixapi</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
```

 - the new public features are in **com.sikulix.api**
 - the current API (up to 1.1.x) in **org.sikuli.script** will be kept for backwards compatibility, but rerouted to **com.sikulix.api/core** as needed
 - new projects using Java should use the API as provided by **com.sikulix.api**
 
 **Related subprojects** (available on OSSRH/MavenCentral)
 
 [Sikulix2openCV](https://github.com/RaiMan/Sikulix2opencv) OpenCV Java API and prebuilt libraries<br>
 [Sikulix2tesseract](https://github.com/RaiMan/Sikulix2tesseract) Tess4J Java API and prebuilt libraries<br>
 [Sikulix2TigerVNC](https://github.com/RaiMan/Sikulix2tigervnc) The TigerVNC Java implementation for VNC support
 
Prerequisites for development and testing
---

 - a Java JDK 1.8
 - Maven 3+
 - only 64-Bit Systems supported

**For developement I use the [JetBrains IDEs](https://www.jetbrains.com)**

 - **[IntelliJ IDEA CE](https://www.jetbrains.com/idea/)** for Java and everything else
 - **[PyCharm CE](https://www.jetbrains.com/pycharm/)** for special Jython/Python stuff
 - **[RubyMine](https://www.jetbrains.com/ruby/)** for special JRuby/Ruby stuff (special license for OpenSource projects)
 
Be aware for development and testing
---

 - the so called folder `SikulixAppData`, where SikuliX specific stuff is stored once per machine, for SikuliX2 now is `SikulixAppData/SX2` to allow the parallel usage of SikuliX1 and SikuliX2 on the same machine.
