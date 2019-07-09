package org.ekrich.tensorflow.snic

import scalanative.unsafe._
import utest._
import org.ekrich.tensorflow.snic.tensorflow._
import org.ekrich.tensorflow.snic.tensorflowEnums._
import scalanative.libc.stdlib._
import scalanative.unsafe.CFuncPtr3

object TensorflowSuite extends TestSuite {

  // def DeallocateTensor(data: Ptr[Byte], sz: CSize, unk: Ptr[Byte]): Unit = {
  //   free(data)
  //   println("Deallocate tensor")
  // }

  val DeallocateTensor = new CFuncPtr3[Ptr[Byte], CSize, Ptr[Byte], Unit] {
    def apply(data: Ptr[Byte], sz: CSize, unk: Ptr[Byte]): Unit = {
      //free(data)
      println("Free Tensor")
    }
  }

  val tests = this {
    'TF_Version {
      Zone { implicit z =>
        println(s"Tensorflow version: ${fromCString(TF_Version())}")
        assert("1.13.1" == fromCString(TF_Version()))
      }
    }
    'TF_Example {
      Zone { implicit z =>
        println("Running example...")
        assert("1.13.1" == fromCString(TF_Version()))

        // handle dims
        val dimsVals = Seq(1, 5, 12)
        val dimsSize = dimsVals.size
        val dims     = alloc[int64_t](sizeof[int64_t] * dimsSize)
        for (i <- 0 until dimsSize) {
          dims(i) = dimsVals(i)
        }

        // handle data based on dims
        var dataSize = sizeof[CFloat]
        for (i <- dimsVals) dataSize *= i
        println(s"data_size: $dataSize")

        val data = alloc[CFloat](dataSize)

        val dataVals = Seq(
          -0.4809832f, -0.3770838f, 0.1743573f, 0.7720509f, -0.4064746f,
          0.0116595f, 0.0051413f, 0.9135732f, 0.7197526f, -0.0400658f,
          0.1180671f, -0.6829428f, -0.4810135f, -0.3772099f, 0.1745346f,
          0.7719303f, -0.4066443f, 0.0114614f, 0.0051195f, 0.9135003f,
          0.7196983f, -0.0400035f, 0.1178188f, -0.6830465f, -0.4809143f,
          -0.3773398f, 0.1746384f, 0.7719052f, -0.4067171f, 0.0111654f,
          0.0054433f, 0.9134697f, 0.7192584f, -0.0399981f, 0.1177435f,
          -0.6835230f, -0.4808300f, -0.3774327f, 0.1748246f, 0.7718700f,
          -0.4070232f, 0.0109549f, 0.0059128f, 0.9133330f, 0.7188759f,
          -0.0398740f, 0.1181437f, -0.6838635f, -0.4807833f, -0.3775733f,
          0.1748378f, 0.7718275f, -0.4073670f, 0.0107582f, 0.0062978f,
          0.9131795f, 0.7187147f, -0.0394935f, 0.1184392f, -0.6840039f
        )
        // copy to memory
        for (i <- 0 until dataVals.size) {
          data(i) = dataVals(i)
        }

        println(dimsVals)
        println(dims)

        println(dataVals)
        println(data)

        // /**
        //  * Return a new tensor that holds the bytes data[0,len-1].
        //  *
        //  *  The data will be deallocated by a subsequent call to TF_DeleteTensor via:
        //  *       (*deallocator)(data, len, deallocator_arg)
        //  *  Clients must provide a custom deallocator function so they can pass in
        //  *  memory managed by something like numpy.
        //  *
        //  *  May return NULL (and invoke the deallocator) if the provided data buffer
        //  *  (data, len) is inconsistent with a tensor of the given TF_DataType
        //  *  and the shape specified by (dima, num_dims).
        //  */
        // def TF_NewTensor(
        //     value: TF_DataType,
        //     dims: Ptr[int64_t],
        //     num_dims: CInt,
        //     data: Ptr[Byte],
        //     len: CSize,
        //     deallocator: CFuncPtr3[Ptr[Byte], CSize, Ptr[Byte], Unit],
        //     deallocator_arg: Ptr[Byte]): Ptr[TF_Tensor] = extern

        val nul = alloc[Byte]
        !nul = 0x00

        val tensor = TF_NewTensor(TF_FLOAT,
                                  dims,
                                  dimsSize,
                                  data.asInstanceOf[Ptr[Byte]],
                                  dataSize,
                                  DeallocateTensor,
                                  nul);

        println(s"Tensor: $tensor")
        TF_DeleteTensor(tensor)
        println("Done.")
      }
    }
  }
}
