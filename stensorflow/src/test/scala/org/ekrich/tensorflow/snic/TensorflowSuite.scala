package org.ekrich.tensorflow.snic

import scalanative.unsafe._
import utest._
import org.ekrich.tensorflow.snic.tensorflow._

object TensorflowSuite extends TestSuite {

  val tests = this {
    'TF_Version {
      Zone { implicit z =>
        println(s"Tensorflow version: ${fromCString(TF_Version())}")
        assert("1.13.1" == fromCString(TF_Version()))
      }
    }
  }
}
