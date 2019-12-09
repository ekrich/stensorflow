package org.ekrich.tensorflow.unsafe

import minitest._

import scalanative.libc.stdlib
import scala.scalanative.unsafe.{CFloat, CFuncPtr3, CSize, fromCString}
import scala.scalanative.unsafe.{Ptr, Zone, alloc, sizeof}

import org.ekrich.tensorflow.unsafe.tensorflow._
import org.ekrich.tensorflow.unsafe.tensorflowEnums._

object TensorflowSuite extends SimpleTestSuite {

  val tfVersion = "2.0.0"

  val deallocateTensor = new CFuncPtr3[Ptr[Byte], CSize, Ptr[Byte], Unit] {
    def apply(data: Ptr[Byte], len: CSize, deallocateArg: Ptr[Byte]): Unit = {
      stdlib.free(data)
      println("Free Original Tensor")
    }
  }

  test("TF_Version") {
    Zone { implicit z =>
      println(s"Tensorflow version: ${fromCString(TF_Version())}")
      assert(tfVersion == fromCString(TF_Version()))
    }
  }
  test("TF_Example") {
    Zone { implicit z =>
      println("Running example...")
      assert(tfVersion == fromCString(TF_Version()))

      // handle dims
      val dimsVals  = Seq(1, 5, 12)
      val dimsSize  = dimsVals.size
      val dimsBytes = dimsSize * sizeof[int64_t]
      //val dims      = alloc[int64_t](dimsSize)
      val dims = stdlib.malloc(dimsBytes).asInstanceOf[Ptr[int64_t]]

      // copy to memory
      for (i <- 0 until dimsSize) {
        dims(i) = dimsVals(i)
      }

      // handle data based on dims
      val dataVals = Seq(
        -0.4809832f, -0.3770838f, 0.1743573f, 0.7720509f, -0.4064746f,
        0.0116595f, 0.0051413f, 0.9135732f, 0.7197526f, -0.0400658f, 0.1180671f,
        -0.6829428f, -0.4810135f, -0.3772099f, 0.1745346f, 0.7719303f,
        -0.4066443f, 0.0114614f, 0.0051195f, 0.9135003f, 0.7196983f,
        -0.0400035f, 0.1178188f, -0.6830465f, -0.4809143f, -0.3773398f,
        0.1746384f, 0.7719052f, -0.4067171f, 0.0111654f, 0.0054433f, 0.9134697f,
        0.7192584f, -0.0399981f, 0.1177435f, -0.6835230f, -0.4808300f,
        -0.3774327f, 0.1748246f, 0.7718700f, -0.4070232f, 0.0109549f,
        0.0059128f, 0.9133330f, 0.7188759f, -0.0398740f, 0.1181437f,
        -0.6838635f, -0.4807833f, -0.3775733f, 0.1748378f, 0.7718275f,
        -0.4073670f, 0.0107582f, 0.0062978f, 0.9131795f, 0.7187147f,
        -0.0394935f, 0.1184392f, -0.6840039f
      )

      // dimensions need to match data
      val dataSize  = dimsVals.reduceLeft(_ * _)
      val dataBytes = dataSize * sizeof[CFloat]
      //val data      = alloc[CFloat](dataSize)
      val data = stdlib.malloc(dataBytes).asInstanceOf[Ptr[CFloat]]

      // copy to memory
      for (i <- 0 until dataSize) {
        data(i) = dataVals(i)
      }

      println(dimsVals)
      println(dimsSize)
      println(dims)

      println(dataVals)
      println(dataSize)
      println(dataBytes)
      println(data)

      // same as null?
      val nullptr = alloc[Byte]
      !nullptr = 0x00

      println("Create Tensor")
      val tensor =
        TF_NewTensor(TF_FLOAT,
                     dims,
                     dimsSize,
                     data.asInstanceOf[Ptr[Byte]],
                     dataBytes,
                     deallocateTensor,
                     nullptr);

      println(s"Tensor: $tensor")

      if (tensor == null) {
        println("Wrong create tensor")
      }

      if (TF_TensorType(tensor) != TF_FLOAT) {
        println("Wrong tensor type")
      }

      if (TF_NumDims(tensor) != dimsSize) {
        println(s"Wrong number of dimensions")
      }

      for (i <- 0 until dimsSize) {
        if (TF_Dim(tensor, i) != dims(i)) {
          println(s"Wrong dimension size for dim: $i")
        }
      }

      if (TF_TensorByteSize(tensor) != dataBytes) {
        println("Wrong tensor byte size")
      }

      val tensor_data = TF_TensorData(tensor).asInstanceOf[Ptr[Float]]

      if (tensor_data == null) {
        println("Wrong data tensor")
      }

      for (i <- 0 until dataSize) {
        if (tensor_data(i) != dataVals(i)) {
          println(s"Element: $i does not match")
        }
      }

      println("Success create tensor")
      TF_DeleteTensor(tensor)
      println("Done.")
    }
  }
}
