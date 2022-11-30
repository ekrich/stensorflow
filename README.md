# stensorflow - Scala Native TensorFlow
![CI](https://github.com/ekrich/stensorflow/workflows/CI/badge.svg)

This library implements the C TensorFlow API adapted for the Scala Native platform.

Scala Native is a unique platform that
marries the high level language of Scala but compiles to native code with a
lightweight managed runtime which includes a state of the art garbage collector.
The combination allows for great programming and the ability to use high
performance C language libraries like TensorFlow.

Scala Native uses the Scala compiler to produce
[NIR](https://scala-native.readthedocs.io/en/latest/contrib/nir.html)
(Native Intermediate Representation) that is optimized and then
converted to [LLVM IR](http://llvm.org/). Finally LLVM code is optimized
and compiled by [Clang](http://clang.llvm.org/) to produce a native executable.

## Getting started
[![Maven Central](https://img.shields.io/maven-central/v/org.ekrich/stensorflow_native0.4_3.svg)](https://maven-badges.herokuapp.com/maven-central/org.ekrich/stensorflow_native0.4_3)

If you are already familiar with Scala Native you can jump right in by adding the following dependency in your `sbt` build file.

```scala
libraryDependencies += "org.ekrich" %%% "stensorflow" % "x.y.z"
```

To use in `sbt`, replace `x.y.z` with the version from Maven Central badge above.
All available versions can be seen at the [Maven Repository](https://mvnrepository.com/artifact/org.ekrich/stensorflow).

Otherwise follow the [Getting Started](https://scala-native.readthedocs.io/en/latest/user/setup.html)
instructions for Scala Native if you are not already setup.

## Scala Build Versions

| Scala Version          | Native (0.4.9+)       |
| ---------------------- | :-------------------: |
| 3.2.x                  |          ✅           |

Use version `0.3.0` or greater for Scala Native `0.4.9+` and Scala 3.
Refer to release notes for older versions of Scala and Scala Native

## Additional libraries

The TensorFlow C library is required and the current version is `2.11.0`.

* Linux/Ubuntu can TensorFlow following the following directions:

https://www.tensorflow.org/install/lang_c

The essential directions are repeated here replacing `<version>` with the above:

```
$ FILENAME=libtensorflow-cpu-linux-x86_64-<version>.tar.gz
$ wget -q --no-check-certificate https://storage.googleapis.com/tensorflow/libtensorflow/${FILENAME}
$ sudo tar -C /usr/local -xzf ${FILENAME}
$ sudo ldconfig /usr/local/lib
```

* macOS can install TensorFlow using [Homebrew](https://formulae.brew.sh/formula/libtensorflow) 
which will install into the `/usr/local/Cellar/libtensorflow/<version>` directory.

Note: macOS 11 or greater is recommended to install TensorFlow via
Homebrew and is used in CI.

```
$ brew install libtensorflow
```

* Other OSes need to have `libtensorflow` available on the system.

## Usage and Help
[![scaladoc](https://www.javadoc.io/badge/org.ekrich/stensorflow_native0.4_3.svg?label=scaladoc)](https://www.javadoc.io/doc/org.ekrich/stensorflow_native0.4_3)
[![Join chat https://gitter.im/ekrich/stensorflow](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/ekrich/stensorflow?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Reference the link above for Scaladoc. The documentation is a little sparse but hopefully will improve with time.

After `sbt` is installed and any other Scala Native prerequisites are met you can use the following Gitter G8 template instructions to get a fully functional Scala Native application with an example in the body of the main program.

```
$ sbt new ekrich/stensorflow.g8
$ cd <directory entered after the prompt>
$ sbt run
```

In addition, look at the [stensorflow unit tests](https://github.com/ekrich/stensorflow/blob/main/stensorflow/src/test/scala/org/ekrich/tensorflow/unsafe/TensorflowTest.scala) for other examples of usage.

## TensorFlow References and External Documentation

[TensorFlow Website](https://www.tensorflow.org/)<br/>
[TensorFlow for JVM using JNI](http://platanios.org/tensorflow_scala/)

## Versions

Release [0.3.0](https://github.com/ekrich/stensorflow/releases/tag/v0.3.0) - (2022-11-29)<br/>
Release [0.2.0](https://github.com/ekrich/stensorflow/releases/tag/v0.2.0) - (2021-12-13)<br/>
Release [0.1.0](https://github.com/ekrich/stensorflow/releases/tag/v0.1.0) - (2021-07-02)<br/>
