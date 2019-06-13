#!/usr/bin/env bash
# Similar to Scala Native

# Enable strict mode and fail the script on non-zero exit code,
# unresolved variable or pipe failure.
set -euo pipefail
IFS=$'\n\t'

if [[ "$TRAVIS_OS_NAME" == "osx" ]]; then
    brew install re2
    brew install libtensorflow
else
    # Per https://github.com/scala-native/scala-native/pull/1240/
    sudo apt-get update

    # Remove libunwind pre-bundled with clang
    sudo find /usr -name "*libunwind*" -delete

    # Use pre-bundled clang
    export PATH=/usr/local/clang-5.0.0/bin:$PATH
    export CXX=clang++

    # Install Boehm GC and libunwind
    sudo apt-get install libgc-dev libunwind8-dev

    # Install tensorflow
    sudo curl -O https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow-cpu-linux-x86_64-1.13.1.tar.gz
    sudo tar -C /usr/local -xzf libtensorflow-cpu-linux-x86_64-1.13.1.tar.gz
    sudo ldconfig

    # Install re2
    # Starting from Ubuntu 16.04 LTS, it'll be available as http://packages.ubuntu.com/xenial/libre2-dev
    sudo apt-get install -y make
    git clone https://code.googlesource.com/re2
    pushd re2
    git checkout 2017-03-01
    make -j4 test
    sudo make install prefix=/usr
    make testinstall prefix=/usr
    popd
fi
