# stensorflow - Scala Native TensorFlow
[![Build Status](https://travis-ci.org/ekrich/sblas.svg?branch=master)](https://travis-ci.org/ekrich/stensorflow)

This library implements TensorFlow 1.13.1 (Basic Linear Algebra Subprograms) in the form
of CBLAS for the Scala Native platform. Scala Native is a unique platform that
marries the high level language of Scala but compiles to native code with a
lightweight managed runtime which includes a state of the art garbage collector.
The combination allows for great programming and the ability to use high
performance C language libraries like CBLAS.

Scala Native uses the Scala compiler to produce
[NIR](https://scala-native.readthedocs.io/en/latest/contrib/nir.html)
(Native Intermediate Representation) that is optimized and then
converted to [LLVM IR](http://llvm.org/). Finally LLVM code is optimized
and compiled by [Clang](http://clang.llvm.org/) to produce a native executable.

## Getting started
[![Maven Central](https://img.shields.io/maven-central/v/org.ekrich/sblas_native0.3_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/org.ekrich/sblas_native0.3_2.11)

If you are already familiar with Scala Native you can jump right in by adding the following dependency in your `sbt` build file.

```scala
libraryDependencies += "org.ekrich" %%% "sblas" % "x.y.z"
```

To use in `sbt`, replace `x.y.z` with the version from Maven Central badge above.
All available versions can be seen at the [Maven Repository](https://mvnrepository.com/artifact/org.ekrich/sblas).

Otherwise follow the [Getting Started](https://scala-native.readthedocs.io/en/latest/user/setup.html)
instructions for Scala Native if you are not already setup.

Additional libraries that need to be installed on you system are as follows:

* Linux/Ubuntu can TensorFlow as follows:

```
$ sudo apt-get install libatlas-base-dev
```

* macOS can install TensorFlow using [Homebrew](https://formulae.brew.sh/formula/libtensorflow) which will install into the `/usr/local/Cellar/libtensorflow/<version>` directory.

```
$ brew install libtensorflow
```

* Other OSes need to have `libtensorflow` available on the system.

## Usage and Help
[![scaladoc](https://www.javadoc.io/badge/org.ekrich/sblas_native0.3_2.11.svg?label=scaladoc)](https://www.javadoc.io/doc/org.ekrich/sblas_native0.3_2.11)
[![Join chat https://gitter.im/ekrich/sblas](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/ekrich/sblas?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Reference the link above for Scaladoc. The documentation is a little sparse but hopefully will improve with time.

After `sbt` is installed and any other Scala Native prerequisites are met you can use the following Gitter G8 template instructions to get a fully functional Scala Native application with a couple of BLAS examples in the body of the main program.

```
$ sbt new ekrich/sblas.g8
$ cd <directory entered after the prompt>
$ sbt run
```

In addition, look at the `v0.1.1` [sblas unit tests](https://github.com/ekrich/sblas/blob/v0.1.1/sblas/src/test/scala/org/ekrich/blas/snic/BlasSuite.scala) for other examples of usage.

## BLAS References and External Documentation

Some useful links are as follows which are also in the LICENSE.md file as some of the Scaladoc was sourced from these references:

Wikipedia Website:
- https://en.wikipedia.org/wiki/Basic_Linear_Algebra_Subprograms

Netlib Website:
- http://www.netlib.org/blas/#_documentation
- http://www.netlib.org/blas/#_blas_routines
- http://www.netlib.org/lapack/lapack-3.1.1/html/

Apple Website:
- https://developer.apple.com/documentation/accelerate/blas?language=objc

Intel Website:
- https://software.intel.com/en-us/mkl-developer-reference-c-blas-routines
- https://software.intel.com/en-us/mkl-developer-reference-c-naming-conventions-for-blas-routines

IBM Website:
- https://www.ibm.com/support/knowledgecenter/en/SSFHY8_6.1/reference/am5gr_apa.html

## Versions

Release [0.1.1](https://github.com/ekrich/sblas/releases/tag/v0.1.1) - (2019-05-01)<br/>
