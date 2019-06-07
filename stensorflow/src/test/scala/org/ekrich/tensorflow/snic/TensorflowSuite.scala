package org.ekrich.tensorflow.snic

import utest._
import org.ekrich.tensorflow.snic.tensorflow._

object TensorflowSuite extends TestSuite {

  val tests = this {

    'test {
      Zone { implicit z =>
       
        assert(3.0 == 3.0)
      }
    }
  }
}
