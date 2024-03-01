/**
 *    - Copyright 2015 The TensorFlow Authors. All Rights Reserved.
 *    - Copyright 2017-2022 Eric K Richardson
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */
package org.ekrich.tensorflow.unsafe

import scalanative.unsafe._

/** Enums used in the API
 */
object tensorflowEnums {

  /** TF_DataType holds the type for a scalar value. E.g., one slot in a tensor.
   *  The enum values here are identical to corresponding values in types.proto.
   */
  type TF_DataType = CInt
  final val TF_FLOAT: TF_DataType = 1
  final val TF_DOUBLE: TF_DataType = 2
  // Int32 tensors are always in 'host' memory.
  final val TF_INT32: TF_DataType = 3
  final val TF_UINT8: TF_DataType = 4
  final val TF_INT16: TF_DataType = 5
  final val TF_INT8: TF_DataType = 6
  final val TF_STRING: TF_DataType = 7
  final val TF_COMPLEX64: TF_DataType = 8 // Single-precision complex
  // Old identifier kept for API backwards compatibility
  final val TF_COMPLEX: TF_DataType = 8
  final val TF_INT64: TF_DataType = 9
  final val TF_BOOL: TF_DataType = 10
  final val TF_QINT8: TF_DataType = 11 // Quantized int8
  final val TF_QUINT8: TF_DataType = 12 // Quantized uint8
  final val TF_QINT32: TF_DataType = 13 // Quantized int32
  // Float32 truncated to 16 bits. Only for cast ops.
  final val TF_BFLOAT16: TF_DataType = 14
  final val TF_QINT16: TF_DataType = 15 // Quantized int16
  final val TF_QUINT16: TF_DataType = 16 // Quantized uint16
  final val TF_UINT16: TF_DataType = 17
  final val TF_COMPLEX128: TF_DataType = 18 // Double-precision complex
  final val TF_HALF: TF_DataType = 19
  final val TF_RESOURCE: TF_DataType = 20
  final val TF_VARIANT: TF_DataType = 21
  final val TF_UINT32: TF_DataType = 22
  final val TF_UINT64: TF_DataType = 23

  /** TF_Code holds an error code. The enum values here are identical to
   *  corresponding values in error_codes.proto.
   */
  type TF_Code = CInt
  final val TF_OK: TF_Code = 0
  final val TF_CANCELLED: TF_Code = 1
  final val TF_UNKNOWN: TF_Code = 2
  final val TF_INVALID_ARGUMENT: TF_Code = 3
  final val TF_DEADLINE_EXCEEDED: TF_Code = 4
  final val TF_NOT_FOUND: TF_Code = 5
  final val TF_ALREADY_EXISTS: TF_Code = 6
  final val TF_PERMISSION_DENIED: TF_Code = 7
  final val TF_UNAUTHENTICATED: TF_Code = 16
  final val TF_RESOURCE_EXHAUSTED: TF_Code = 8
  final val TF_FAILED_PRECONDITION: TF_Code = 9
  final val TF_ABORTED: TF_Code = 10
  final val TF_OUT_OF_RANGE: TF_Code = 11
  final val TF_UNIMPLEMENTED: TF_Code = 12
  final val TF_INTERNAL: TF_Code = 13
  final val TF_UNAVAILABLE: TF_Code = 14
  final val TF_DATA_LOSS: TF_Code = 15

  /** TF_AttrType describes the type of the value of an attribute on an
   *  operation.
   */
  type TF_AttrType = CInt
  final val TF_ATTR_STRING: TF_AttrType = 0
  final val TF_ATTR_INT: TF_AttrType = 1
  final val TF_ATTR_FLOAT: TF_AttrType = 2
  final val TF_ATTR_BOOL: TF_AttrType = 3
  final val TF_ATTR_TYPE: TF_AttrType = 4
  final val TF_ATTR_SHAPE: TF_AttrType = 5
  final val TF_ATTR_TENSOR: TF_AttrType = 6
  final val TF_ATTR_PLACEHOLDER: TF_AttrType = 7
  final val TF_ATTR_FUNC: TF_AttrType = 8
}

import tensorflowEnums._

/** C API for TensorFlow.
 *
 *  The API leans towards simplicity and uniformity instead of convenience since
 *  most usage will be by language specific wrappers.
 *
 *  Conventions:
 *    - We use the prefix TF_ for everything in the API.
 *    - Objects are always passed around as pointers to opaque structs and these
 *      structs are allocated/deallocated via the API.
 *    - TF_Status holds error information. It is an object type and therefore is
 *      passed around as a pointer to an opaque struct as mentioned above.
 *    - Every call that has a TF_Status* argument clears it on success and fills
 *      it with error info on failure.
 *    - unsigned char is used for booleans (instead of the 'bool' type). In C++
 *      bool is a keyword while in C99 bool is a macro defined in stdbool.h. It
 *      is possible for the two to be inconsistent. For example, neither the C99
 *      nor the C++11 standard force a byte size on the bool type, so the macro
 *      defined in stdbool.h could be inconsistent with the bool keyword in C++.
 *      Thus, the use of stdbool.h is avoided and unsigned char is used instead.
 *    - size_t is used to represent byte sizes of objects that are materialized
 *      in the address space of the calling process.
 *    - int is used as an index into arrays.
 *    - Deletion functions are safe to call on nullptr.
 *
 *  Questions left to address:
 *    - Might at some point need a way for callers to provide their own Env.
 *    - Maybe add TF_TensorShape that encapsulates dimension info.
 *
 *  Design decisions made:
 *    - Backing store for tensor memory has an associated deallocation function.
 *      This deallocation function will point to client code for tensors
 *      populated by the client. So the client can do things like shadowing a
 *      numpy array.
 *    - We do not provide TF_OK since it is not strictly necessary and we are
 *      not optimizing for convenience.
 *    - We make assumption that one session has one graph. This should be fine
 *      since we have the ability to run sub-graphs.
 *    - We could allow NULL for some arguments (e.g., NULL options arg). However
 *      since convenience is not a primary goal, we don't do this.
 *    - Devices are not in this API. Instead, they are created/used internally
 *      and the API just provides high level controls over the number of devices
 *      of each type.
 */
@link("tensorflow")
@extern
object tensorflow {

  type int64_t = CLongLong
  type uint64_t = CUnsignedLongLong

  /** TF_Status holds error information. It either has an OK code, or else an
   *  error code with an associated error message.
   */
  type TF_Status = CStruct0

  /** Represents a computation graph. Graphs may be shared between sessions.
   *  Graphs are thread-safe when used as directed below.
   */
  type TF_Graph = CStruct0

  /** Operation that has been added to the graph. Valid until the graph is
   *  deleted -- in particular adding a new operation to the graph does not
   *  invalidate old TF_Operation* pointers.
   */
  type TF_Operation = CStruct0

  /** Operation being built. The underlying graph must outlive this.
   */
  type TF_OperationDescription = CStruct0

  /** TF_Tensor holds a multi-dimensional array of elements of a single data
   *  type. For all types other than TF_STRING, the data buffer stores elements
   *  in row major order. E.g. if data is treated as a vector of TF_DataType:
   *
   *    - element 0: index (0, ..., 0)
   *    - element 1: index (0, ..., 1)
   *    - ...
   *
   *  The format for TF_STRING tensors is:
   *    - start_offset: array[uint64]
   *    - data: byte[...]
   *
   *  The string length (as a varint), followed by the contents of the string is
   *  encoded at data[start_offset[i]]]. TF_StringEncode and TF_StringDecode
   *  facilitate this encoding.
   */
  type TF_Tensor = CStruct0

  /** TF_SessionOptions holds options that can be passed during session
   *  creation.
   */
  type TF_SessionOptions = CStruct0

  /** TF_Buffer holds a pointer to a block of data and its associated length.
   *  Typically, the data consists of a serialized protocol buffer, but other
   *  data may also be held in a buffer.
   *
   *  By default, TF_Buffer itself does not do any memory management of the
   *  pointed-to block. If need be, users of this struct should specify how to
   *  deallocate the block by setting the `data_deallocator` function pointer.
   */
  type TF_Buffer =
    CStruct3[Ptr[Byte], CSize, CFuncPtr2[Ptr[Byte], CSize, Unit]]

  /** Represents a specific input of an operation.
   */
  type TF_Input = CStruct2[Ptr[TF_Operation], CInt]

  /** Represents a specific output of an operation.
   */
  type TF_Output = CStruct2[Ptr[TF_Operation], CInt]

  /** TF_Function is a grouping of operations with defined inputs and outputs.
   *  Once created and added to graphs, functions can be invoked by creating an
   *  operation whose operation type matches the function name.
   */
  type TF_Function = CStruct0

  /** Function definition options.
   */
  type TF_FunctionOptions = CStruct0

  /** TF_AttrMetadata describes the value of an attribute on an operation.
   */
  type TF_AttrMetadata = CStruct4[
    /** A boolean: 1 if the attribute value is a list, 0 otherwise. */
    CUnsignedChar,
    /** Length of the list if is_list is true. Undefined otherwise. */
    int64_t,
    /** Type of elements of the list if is_list != 0.
     *
     *  Type of the single value stored in the attribute if is_list == 0.
     */
    TF_AttrType,
    /** Total size the attribute value. The units of total_size depend on
     *  is_list and type.
     *    - (1) If type == TF_ATTR_STRING and is_list == 0 then total_size is
     *      the byte size of the string valued attribute.
     *    - (2) If type == TF_ATTR_STRING and is_list == 1 then total_size is
     *      the cumulative byte size of all the strings in the list.
     *    - (3) If type == TF_ATTR_SHAPE and is_list == 0 then total_size is the
     *      number of dimensions of the shape valued attribute, or -1 if its
     *      rank is unknown.
     *    - (4) If type == TF_ATTR_SHAPE and is_list == 1 then total_size is the
     *      cumulative number of dimensions of all shapes in the list.
     *    - (5) Otherwise, total_size is undefined.
     */
    int64_t
  ]

  type TF_WhileParams = CStruct8[
    /** The number of inputs to the while loop, i.e. the number of loop
     *  variables. This is the size of cond_inputs, body_inputs, and
     *  body_outputs.
     */
    CInt, // ninputs
    /** The while condition graph. The inputs are the current values of the loop
     *  variables. The output should be a scalar boolean.
     */
    Ptr[TF_Graph], // cond_graph
    Ptr[TF_Output], // cond_inputs
    Ptr[TF_Output], // cond_output // TF_output
    /** The loop body graph. The inputs are the current values of the loop
     *  variables. The outputs are the updated values of the loop variables.
     */
    Ptr[TF_Graph], // body_graph
    Ptr[TF_Output], // body_inputs
    Ptr[TF_Output], // body_outputs
    /** The loop body graph. The inputs are the current values of the loop
     *  variables. The outputs are the updated values of the loop variables.
     */
    CString // name
  ]

  /** TF_Version returns a string describing version information of the
   *  TensorFlow library. TensorFlow using semantic versioning.
   */
  def TF_Version(): CString = extern

  /** TF_DataTypeSize returns the sizeof() for the underlying type corresponding
   *  to the given TF_DataType enum value. Returns 0 for variable length types
   *  (eg. TF_STRING) or on failure.
   */
  def TF_DataTypeSize(value: TF_DataType): CSize = extern

  /** Return a new status object.
   */
  def TF_NewStatus(): Ptr[TF_Status] = extern

  /** Delete a previously created status object.
   */
  def TF_DeleteStatus(status: Ptr[TF_Status]): Unit = extern

  /** Record <code, msg> in *s. Any previous information is lost. A common use
   *  is to clear a status: TF_SetStatus(s, TF_OK, "");
   */
  def TF_SetStatus(s: Ptr[TF_Status], code: TF_Code, msg: CString): Unit =
    extern

  /** Return the code record in *s.
   */
  def TF_GetCode(s: Ptr[TF_Status]): TF_Code = extern

  /** Return a pointer to the (null-terminated) error message in *s. The return
   *  value points to memory that is only usable until the next mutation to *s.
   *  Always returns an empty string if TF_GetCode(s) is TF_OK.
   */
  def TF_Message(s: Ptr[TF_Status]): CString = extern

  /** Makes a copy of the input and sets an appropriate deallocator. Useful for
   *  passing in read-only, input protobufs.
   */
  def TF_NewBufferFromString(
      proto: Ptr[Byte],
      proto_len: CSize
  ): Ptr[TF_Buffer] = extern

  /** Useful for passing *out* a protobuf.
   */
  def TF_NewBuffer(): Ptr[TF_Buffer] = extern
  def TF_DeleteBuffer(buffer: Ptr[TF_Buffer]): Unit = extern
  def TF_GetBuffer(buffer: Ptr[TF_Buffer]): TF_Buffer = extern

  /** Return a new tensor that holds the bytes data[0,len-1].
   *
   *  The data will be deallocated by a subsequent call to TF_DeleteTensor via:
   *  (*deallocator)(data, len, deallocator_arg) Clients must provide a custom
   *  deallocator function so they can pass in memory managed by something like
   *  numpy.
   *
   *  May return NULL (and invoke the deallocator) if the provided data buffer
   *  (data, len) is inconsistent with a tensor of the given TF_DataType and the
   *  shape specified by (dims, num_dims).
   */
  def TF_NewTensor(
      value: TF_DataType,
      dims: Ptr[int64_t],
      num_dims: CInt,
      data: Ptr[Byte],
      len: CSize,
      deallocator: CFuncPtr3[Ptr[Byte], CSize, Ptr[Byte], Unit],
      deallocator_arg: Ptr[Byte]
  ): Ptr[TF_Tensor] = extern

  /** Allocate and return a new Tensor.
   *
   *  This function is an alternative to TF_NewTensor and should be used when
   *  memory is allocated to pass the Tensor to the C API. The allocated memory
   *  satisfies TensorFlow's memory alignment preferences and should be
   *  preferred over calling malloc and free.
   *
   *  The caller must set the Tensor values by writing them to the pointer
   *  returned by TF_TensorData with length TF_TensorByteSize.
   */
  def TF_AllocateTensor(
      value: TF_DataType,
      dims: Ptr[int64_t],
      num_dims: CInt,
      len: CSize
  ): Ptr[TF_Tensor] = extern

  /** Deletes `tensor` and returns a new TF_Tensor with the same content if
   *  possible. Returns nullptr and leaves `tensor` untouched if not.
   */
  def TF_TensorMaybeMove(tensor: Ptr[TF_Tensor]): Ptr[TF_Tensor] = extern

  /** Destroy a tensor.
   */
  def TF_DeleteTensor(tensor: Ptr[TF_Tensor]): Unit = extern

  /** Return the type of a tensor element.
   */
  def TF_TensorType(tensor: Ptr[TF_Tensor]): TF_DataType = extern

  /** Return the number of dimensions that the tensor has.
   */
  def TF_NumDims(tensor: Ptr[TF_Tensor]): CInt = extern

  /** Return the length of the tensor in the "dim_index" dimension. REQUIRES: 0
   *  <= dim_index < TF_NumDims(tensor)
   */
  def TF_Dim(tensor: Ptr[TF_Tensor], dim_index: CInt): int64_t = extern

  /** Return the size of the underlying data in bytes.
   */
  def TF_TensorByteSize(tensor: Ptr[TF_Tensor]): CSize = extern

  /** Return a pointer to the underlying data buffer.
   */
  def TF_TensorData(tensor: Ptr[TF_Tensor]): Ptr[Byte] = extern

  /** Encode the string `src` (`src_len` bytes long) into `dst` in the format
   *  required by TF_STRING tensors. Does not write to memory more than
   *  `dst_len` bytes beyond `*dst`. `dst_len` should be at least
   *  TF_StringEncodedSize(src_len).
   *
   *  On success returns the size in bytes of the encoded string. Returns an
   *  error into `status` otherwise.
   */
  def TF_StringEncode(
      src: CString,
      src_len: CSize,
      dst: CString,
      dst_len: CSize,
      status: Ptr[TF_Status]
  ): CSize = extern

  /** Decode a string encoded using TF_StringEncode.
   *
   *  On success, sets `*dst` to the start of the decoded string and `*dst_len`
   *  to its length. Returns the number of bytes starting at `src` consumed
   *  while decoding. `*dst` points to memory within the encoded buffer. On
   *  failure, `*dst` and `*dst_len` are undefined and an error is set in
   *  `status`.
   *
   *  Does not read memory more than `src_len` bytes beyond `src`.
   */
  def TF_StringDecode(
      src: CString,
      src_len: CSize,
      dst: Ptr[CString],
      dst_len: Ptr[CSize],
      status: Ptr[TF_Status]
  ): CSize = extern

  /** Return the size in bytes required to encode a string `len` bytes long into
   *  a TF_STRING tensor.
   */
  def TF_StringEncodedSize(len: CSize): CSize = extern

  /** Return a new options object.
   */
  def TF_NewSessionOptions(): Ptr[TF_SessionOptions] = extern

  /** Set the target in TF_SessionOptions.options. target can be empty, a single
   *  entry, or a comma separated list of entries. Each entry is in one of the
   *  following formats:
   *    - "local"
   *    - ip:port
   *    - host:port
   */
  def TF_SetTarget(options: Ptr[TF_SessionOptions], target: CString): Unit =
    extern

  /** Set the config in TF_SessionOptions.options. config should be a serialized
   *  tensorflow.ConfigProto proto. If config was not parsed successfully as a
   *  ConfigProto, record the error information in *status.
   */
  def TF_SetConfig(
      options: Ptr[TF_SessionOptions],
      proto: Ptr[Byte],
      proto_len: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Destroy an options object.
   */
  def TF_DeleteSessionOptions(sessionOptions: Ptr[TF_SessionOptions]): Unit =
    extern

  /** Return a new graph object.
   */
  def TF_NewGraph(): Ptr[TF_Graph] = extern

  /** Destroy an options object. Graph will be deleted once no more TFSession's
   *  are referencing it.
   */
  def TF_DeleteGraph(graph: Ptr[TF_Graph]): Unit = extern

  /** Sets the shape of the Tensor referenced by `output` in `graph` to the
   *  shape described by `dims` and `num_dims`.
   *
   *  If the number of dimensions is unknown, `num_dims` must be set to -1 and
   *  `dims` can be null. If a dimension is unknown, the corresponding entry in
   *  the `dims` array must be -1.
   *
   *  This does not overwrite the existing shape associated with `output`, but
   *  merges the input shape with the existing shape. For example, setting a
   *  shape of [-1, 2] with an existing shape [2, -1] would set a final shape of
   *  [2, 2] based on shape merging semantics.
   *
   *  Returns an error into `status` if:
   *    - `output` is not in `graph`.
   *    - An invalid shape is being set (e.g., the shape being set is
   *      incompatible with the existing shape).
   */
  @name("scalanative_TF_GraphSetTensorShape")
  def TF_GraphSetTensorShape(
      graph: Ptr[TF_Graph],
      output: Ptr[TF_Output], // TF_output
      dims: Ptr[int64_t],
      num_dims: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Returns the number of dimensions of the Tensor referenced by `output` in
   *  `graph`.
   *
   *  If the number of dimensions in the shape is unknown, returns -1.
   *
   *  Returns an error into `status` if:
   *    - `output` is not in `graph`.
   */
  @name("scalanative_TF_GraphGetTensorNumDims")
  def TF_GraphGetTensorNumDims(
      graph: Ptr[TF_Graph],
      output: Ptr[TF_Output], // TF_output
      status: Ptr[TF_Status]
  ): CInt = extern

  /** Returns the shape of the Tensor referenced by `output` in `graph` into
   *  `dims`. `dims` must be an array large enough to hold `num_dims` entries
   *  (e.g., the return value of TF_GraphGetTensorNumDims).
   *
   *  If the number of dimensions in the shape is unknown or the shape is a
   *  scalar, `dims` will remain untouched. Otherwise, each element of `dims`
   *  will be set corresponding to the size of the dimension. An unknown
   *  dimension is represented by `-1`.
   *
   *  Returns an error into `status` if:
   *    - `output` is not in `graph`.
   *    - `num_dims` does not match the actual number of dimensions.
   */
  @name("scalanative_TF_GraphGetTensorShape")
  def TF_GraphGetTensorShape(
      graph: Ptr[TF_Graph],
      output: Ptr[TF_Output], // TF_output
      dims: Ptr[int64_t],
      num_dims: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Operation will only be added to *graph when TF_FinishOperation() is called
   *  (assuming TF_FinishOperation() does not return an error). *graph must not
   *  be deleted until after TF_FinishOperation() is called.
   */
  def TF_NewOperation(
      graph: Ptr[TF_Graph],
      op_type: CString,
      oper_name: CString
  ): Ptr[TF_OperationDescription] = extern

  /** Specify the device for `desc`. Defaults to empty, meaning unconstrained.
   */
  def TF_SetDevice(desc: Ptr[TF_OperationDescription], device: CString): Unit =
    extern

  /** The calls to TF_AddInput and TF_AddInputList must match (in number, order,
   *  and type) the op declaration. For example, the "Concat" op has
   *  registration:
   *  {{{
   *   REGISTER_OP("Concat")
   *       .Input("concat_dim: int32")
   *       .Input("values: N * T")
   *       .Output("output: T")
   *       .Attr("N: int >= 2")
   *       .Attr("T: type");
   *  }}}
   *  that defines two inputs, "concat_dim" and "values" (in that order). You
   *  must use TF_AddInput() for the first input (since it takes a single
   *  tensor), and TF_AddInputList() for the second input (since it takes a
   *  list, even if you were to pass a list with a single tensor), as in:
   *  {{{
   *   TF_OperationDescription* desc = TF_NewOperation(graph, "Concat", "c");
   *   TF_Output concat_dim_input = {...};
   *   TF_AddInput(desc, concat_dim_input);
   *   TF_Output values_inputs[5] = {{...}, ..., {...}};
   *   TF_AddInputList(desc,values_inputs, 5);
   *  }}}
   *  For inputs that take a single tensor.
   */
  @name("scalanative_TF_AddInput")
  def TF_AddInput(
      desc: Ptr[TF_OperationDescription],
      input: Ptr[TF_Output]
  ): Unit =
    extern // TF_output

  /** For inputs that take a list of tensors. inputs must point to
   *  TF_Output[num_inputs].
   */
  def TF_AddInputList(
      desc: Ptr[TF_OperationDescription],
      inputs: Ptr[TF_Output],
      num_inputs: CInt
  ): Unit = extern

  /** Call once per control input to `desc`.
   */
  def TF_AddControlInput(
      desc: Ptr[TF_OperationDescription],
      input: Ptr[TF_Operation]
  ): Unit = extern

  /** Request that `desc` be co-located on the device where `op` is placed.
   *
   *  Use of this is discouraged since the implementation of device placement is
   *  subject to change. Primarily intended for internal libraries
   */
  def TF_ColocateWith(
      desc: Ptr[TF_OperationDescription],
      op: Ptr[TF_Operation]
  ): Unit = extern

  /** Call some TF_SetAttr*() function for every attr that is not inferred from
   *  an input and doesn't have a default value you wish to keep.
   *
   *  `value` must point to a string of length `length` bytes.
   */
  def TF_SetAttrString(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      value: Ptr[Byte],
      length: CSize
  ): Unit = extern

  /** `values` and `lengths` each must have lengths `num_values`. `values[i]`
   *  must point to a string of length `lengths[i]` bytes.
   */
  def TF_SetAttrStringList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      values: Ptr[Ptr[Byte]],
      lengths: Ptr[CSize],
      num_values: CInt
  ): Unit = extern

  /** */
  def TF_SetAttrInt(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      value: int64_t
  ): Unit = extern

  /** */
  def TF_SetAttrIntList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      values: Ptr[int64_t],
      num_values: CInt
  ): Unit = extern

  /** */
  def TF_SetAttrFloat(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      value: CFloat
  ): Unit = extern

  /** */
  def TF_SetAttrFloatList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      values: Ptr[CFloat],
      num_values: CInt
  ): Unit = extern

  /** */
  def TF_SetAttrBool(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      value: CUnsignedChar
  ): Unit = extern

  /** */
  def TF_SetAttrBoolList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      values: Ptr[CUnsignedChar],
      num_values: CInt
  ): Unit = extern

  /** */
  def TF_SetAttrType(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      value: TF_DataType
  ): Unit = extern

  /** */
  def TF_SetAttrTypeList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      values: Ptr[TF_DataType],
      num_values: CInt
  ): Unit = extern

  /** Set a 'func' attribute to the specified name. `value` must point to a
   *  string of length `length` bytes.
   */
  def TF_SetAttrFuncName(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      value: CString,
      length: CSize
  ): Unit = extern

  /** Set `num_dims` to -1 to represent "unknown rank". Otherwise, `dims` points
   *  to an array of length `num_dims`. `dims[i]` must be >= -1, with -1 meaning
   *  "unknown dimension".
   */
  def TF_SetAttrShape(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      dims: Ptr[int64_t],
      num_dims: CInt
  ): Unit = extern

  /** `dims` and `num_dims` must point to arrays of length `num_shapes`. Set
   *  `num_dims[i]` to -1 to represent "unknown rank". Otherwise, `dims[i]`
   *  points to an array of length `num_dims[i]`. `dims[i][j]` must be >= -1,
   *  with -1 meaning "unknown dimension".
   */
  def TF_SetAttrShapeList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      dims: Ptr[Ptr[int64_t]],
      num_dims: Ptr[CInt],
      num_shapes: CInt
  ): Unit = extern

  /** `proto` must point to an array of `proto_len` bytes representing a
   *  binary-serialized TensorShapeProto.
   */
  def TF_SetAttrTensorShapeProto(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      proto: Ptr[Byte],
      proto_len: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** `protos` and `proto_lens` must point to arrays of length `num_shapes`.
   *  `protos[i]` must point to an array of `proto_lens[i]` bytes representing a
   *  binary-serialized TensorShapeProto.
   */
  def TF_SetAttrTensorShapeProtoList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      protos: Ptr[Ptr[Byte]],
      proto_lens: Ptr[CSize],
      num_shapes: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_SetAttrTensor(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      value: Ptr[TF_Tensor],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_SetAttrTensorList(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      values: Ptr[Ptr[TF_Tensor]],
      num_values: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** `proto` should point to a sequence of bytes of length `proto_len`
   *  representing a binary serialization of an AttrValue protocol buffer.
   */
  def TF_SetAttrValueProto(
      desc: Ptr[TF_OperationDescription],
      attr_name: CString,
      proto: Ptr[Byte],
      proto_len: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** If this function succeeds:
   *    - *status is set to an OK value,
   *    - a TF_Operation is added to the graph,
   *    - a non-null value pointing to the added operation is returned -- this
   *      value is valid until the underlying graph is deleted. Otherwise:
   *    - *status is set to a non-OK value,
   *    - the graph is not modified,
   *    - a null value is returned. In either case, it deletes `desc`.
   */
  def TF_FinishOperation(
      desc: Ptr[TF_OperationDescription],
      status: Ptr[TF_Status]
  ): Ptr[TF_Operation] = extern

  /** TF_Operation functions. Operations are immutable once created, so these
   *  are all query functions.
   */
  def TF_OperationName(oper: Ptr[TF_Operation]): CString = extern

  /** */
  def TF_OperationOpType(oper: Ptr[TF_Operation]): CString = extern

  /** */
  def TF_OperationDevice(oper: Ptr[TF_Operation]): CString = extern

  /** */
  def TF_OperationNumOutputs(oper: Ptr[TF_Operation]): CInt = extern

  /** */
  @name("scalanative_TF_OperationOutputType")
  def TF_OperationOutputType(oper_out: Ptr[TF_Output]): TF_DataType =
    extern // TF_output

  /** */
  def TF_OperationOutputListLength(
      oper: Ptr[TF_Operation],
      arg_name: CString,
      status: Ptr[TF_Status]
  ): CInt = extern

  /** */
  def TF_OperationNumInputs(oper: Ptr[TF_Operation]): CInt = extern

  /** */
  def TF_OperationInputType(oper_in: Ptr[TF_Input]): TF_DataType =
    extern // TF_Input

  /** */
  def TF_OperationInputListLength(
      oper: Ptr[TF_Operation],
      arg_name: CString,
      status: Ptr[TF_Status]
  ): CInt = extern

  /** In this code:
   *  {{{
   *   TF_Output producer = TF_OperationInput(consumer);
   *  }}}
   *  There is an edge from producer.oper's output (given by producer.index) to
   *  consumer.oper's input (given by consumer.index).
   *
   *  Note: for Scala Native we need to pass an additonal Ptr[TF_Output] to
   *  capture the original rvalue (stack, pass by value).
   */
  @name("scalanative_TF_OperationInput")
  def TF_OperationInput(
      oper_in: Ptr[TF_Input],
      oper_out: Ptr[TF_Output]
  ): Ptr[TF_Output] =
    extern // TF_Input TF_Output

  /** Get the number of current consumers of a specific output of an operation.
   *  Note that this number can change when new operations are added to the
   *  graph.
   */
  @name("scalanative_TF_OperationOutputNumConsumers")
  def TF_OperationOutputNumConsumers(oper_out: Ptr[TF_Output]): CInt =
    extern // TF_output

  /** Get list of all current consumers of a specific output of an operation.
   *  `consumers` must point to an array of length at least `max_consumers`
   *  (ideally set to TF_OperationOutputNumConsumers(oper_out)). Beware that a
   *  concurrent modification of the graph can increase the number of consumers
   *  of an operation. Returns the number of output consumers (should match
   *  TF_OperationOutputNumConsumers(oper_out)).
   */
  @name("scalanative_TF_OperationOutputConsumers")
  def TF_OperationOutputConsumers(
      oper_out: Ptr[TF_Output], // TF_output
      consumers: Ptr[TF_Input],
      max_consumers: CInt
  ): CInt = extern

  /** Get the number of control inputs to an operation.
   */
  def TF_OperationNumControlInputs(oper: Ptr[TF_Operation]): CInt = extern

  /** Get list of all control inputs to an operation. `control_inputs` must
   *  point to an array of length `max_control_inputs` (ideally set to
   *  TF_OperationNumControlInputs(oper)). Returns the number of control inputs
   *  (should match TF_OperationNumControlInputs(oper)).
   */
  def TF_OperationGetControlInputs(
      oper: Ptr[TF_Operation],
      control_inputs: Ptr[Ptr[TF_Operation]],
      max_control_inputs: CInt
  ): CInt = extern

  /** Get the number of operations that have `*oper` as a control input. Note
   *  that this number can change when new operations are added to the graph.
   */
  def TF_OperationNumControlOutputs(oper: Ptr[TF_Operation]): CInt = extern

  /** Get the list of operations that have `*oper` as a control input.
   *  `control_outputs` must point to an array of length at least
   *  `max_control_outputs` (ideally set to
   *  TF_OperationNumControlOutputs(oper)). Beware that a concurrent
   *  modification of the graph can increase the number of control outputs.
   *  Returns the number of control outputs (should match
   *  TF_OperationNumControlOutputs(oper)).
   */
  def TF_OperationGetControlOutputs(
      oper: Ptr[TF_Operation],
      control_outputs: Ptr[Ptr[TF_Operation]],
      max_control_outputs: CInt
  ): CInt = extern

  /** Returns metadata about the value of the attribute `attr_name` of `oper`.
   */
  def TF_OperationGetAttrMetadata(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      status: Ptr[TF_Status]
  ): TF_AttrMetadata =
    extern

  /** Fills in `value` with the value of the attribute `attr_name`. `value` must
   *  point to an array of length at least `max_length` (ideally set to
   *  TF_AttrMetadata.total_size from TF_OperationGetAttrMetadata(oper,
   *  attr_name)).
   */
  def TF_OperationGetAttrString(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[Byte],
      max_length: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Get the list of strings in the value of the attribute `attr_name`. Fills
   *  in `values` and `lengths`, each of which must point to an array of length
   *  at least `max_values`.
   *
   *  The elements of values will point to addresses in `storage` which must be
   *  at least `storage_size` bytes in length. Ideally, max_values would be set
   *  to TF_AttrMetadata.list_size and `storage` would be at least
   *  TF_AttrMetadata.total_size, obtained from
   *  TF_OperationGetAttrMetadata(oper, attr_name).
   *
   *  Fails if storage_size is too small to hold the requested number of
   *  strings.
   */
  def TF_OperationGetAttrStringList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      values: Ptr[Ptr[Byte]],
      lengths: Ptr[CSize],
      max_values: CInt,
      storage: Ptr[Byte],
      storage_size: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_OperationGetAttrInt(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[int64_t],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `values` with the value of the attribute `attr_name` of `oper`.
   *  `values` must point to an array of length at least `max_values` (ideally
   *  set TF_AttrMetadata.list_size from TF_OperationGetAttrMetadata(oper,
   *  attr_name)).
   */
  def TF_OperationGetAttrIntList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      values: Ptr[int64_t],
      max_values: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_OperationGetAttrFloat(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[CFloat],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `values` with the value of the attribute `attr_name` of `oper`.
   *  `values` must point to an array of length at least `max_values` (ideally
   *  set to TF_AttrMetadata.list_size from TF_OperationGetAttrMetadata(oper,
   *  attr_name)).
   */
  def TF_OperationGetAttrFloatList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      values: Ptr[CFloat],
      max_values: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_OperationGetAttrBool(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[CUnsignedChar],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `values` with the value of the attribute `attr_name` of `oper`.
   *  `values` must point to an array of length at least `max_values` (ideally
   *  set to TF_AttrMetadata.list_size from TF_OperationGetAttrMetadata(oper,
   *  attr_name)).
   */
  def TF_OperationGetAttrBoolList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      values: Ptr[CUnsignedChar],
      max_values: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_OperationGetAttrType(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[TF_DataType],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `values` with the value of the attribute `attr_name` of `oper`.
   *  `values` must point to an array of length at least `max_values` (ideally
   *  set to TF_AttrMetadata.list_size from TF_OperationGetAttrMetadata(oper,
   *  attr_name)).
   */
  def TF_OperationGetAttrTypeList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      values: Ptr[TF_DataType],
      max_values: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `value` with the value of the attribute `attr_name` of `oper`.
   *  `values` must point to an array of length at least `num_dims` (ideally set
   *  to TF_Attr_Meta.size from TF_OperationGetAttrMetadata(oper, attr_name)).
   */
  def TF_OperationGetAttrShape(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[int64_t],
      num_dims: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `dims` with the list of shapes in the attribute `attr_name` of
   *  `oper` and `num_dims` with the corresponding number of dimensions. On
   *  return, for every i where `num_dims[i]` > 0, `dims[i]` will be an array of
   *  `num_dims[i]` elements. A value of -1 for `num_dims[i]` indicates that the
   *  i-th shape in the list is unknown.
   *
   *  The elements of `dims` will point to addresses in `storage` which must be
   *  large enough to hold at least `storage_size` int64_ts. Ideally,
   *  `num_shapes` would be set to TF_AttrMetadata.list_size and `storage_size`
   *  would be set to TF_AttrMetadata.total_size from
   *  TF_OperationGetAttrMetadata(oper, attr_name).
   *
   *  Fails if storage_size is insufficient to hold the requested shapes.
   */
  def TF_OperationGetAttrShapeList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      dims: Ptr[Ptr[int64_t]],
      num_dims: Ptr[CInt],
      num_shapes: CInt,
      storage: Ptr[int64_t],
      storage_size: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Sets `value` to the binary-serialized TensorShapeProto of the value of
   *  `attr_name` attribute of `oper`'.
   */
  def TF_OperationGetAttrTensorShapeProto(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `values` with binary-serialized TensorShapeProto values of the
   *  attribute `attr_name` of `oper`. `values` must point to an array of length
   *  at least `num_values` (ideally set to TF_AttrMetadata.list_size from
   *  TF_OperationGetAttrMetadata(oper, attr_name)).
   */
  def TF_OperationGetAttrTensorShapeProtoList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      values: Ptr[Ptr[TF_Buffer]],
      max_values: CInt,
      status: Ptr[TF_Status]
  ): Unit =
    extern

  /** Gets the TF_Tensor valued attribute of `attr_name` of `oper`.
   *
   *  Allocates a new TF_Tensor which the caller is expected to take ownership
   *  of (and can deallocate using TF_DeleteTensor).
   */
  def TF_OperationGetAttrTensor(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      value: Ptr[Ptr[TF_Tensor]],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Fills in `values` with the TF_Tensor values of the attribute `attr_name`
   *  of `oper`. `values` must point to an array of TF_Tensor* of length at
   *  least `max_values` (ideally set to TF_AttrMetadata.list_size from
   *  TF_OperationGetAttrMetadata(oper, attr_name)).
   *
   *  The caller takes ownership of all the non-null TF_Tensor* entries in
   *  `values` (which can be deleted using TF_DeleteTensor(values[i])).
   */
  def TF_OperationGetAttrTensorList(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      values: Ptr[Ptr[TF_Tensor]],
      max_values: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Sets `output_attr_value` to the binary-serialized AttrValue proto
   *  representation of the value of the `attr_name` attr of `oper`.
   */
  def TF_OperationGetAttrValueProto(
      oper: Ptr[TF_Operation],
      attr_name: CString,
      output_attr_value: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Returns the operation in the graph with `oper_name`. Returns nullptr if no
   *  operation found.
   */
  def TF_GraphOperationByName(
      graph: Ptr[TF_Graph],
      oper_name: CString
  ): Ptr[TF_Operation] = extern

  /** Iterate through the operations of a graph. To use:
   *  {{{
   *  size_t pos = 0;
   *  TF_Operation* oper;
   *  while ((oper = TF_GraphNextOperation(graph, &pos)) != nullptr) {
   *    DoSomethingWithOperation(oper);
   *  }
   *  }}}
   */
  def TF_GraphNextOperation(
      graph: Ptr[TF_Graph],
      pos: Ptr[CSize]
  ): Ptr[TF_Operation] = extern

  /** Write out a serialized representation of `graph` (as a GraphDef protocol
   *  message) to `output_graph_def` (allocated by TF_NewBuffer()).
   *  `output_graph_def`'s underlying buffer will be freed when
   *  TF_DeleteBuffer() is called.
   *
   *  May fail on very large graphs in the future.
   */
  def TF_GraphToGraphDef(
      graph: Ptr[TF_Graph],
      output_graph_def: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Returns the serialized OpDef proto with name `op_name`, or a bad status if
   *  no such op exists. This can return OpDefs of functions copied into the
   *  graph.
   */
  def TF_GraphGetOpDef(
      graph: Ptr[TF_Graph],
      op_name: CString,
      output_op_def: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Returns the serialized VersionDef proto for this graph.
   */
  def TF_GraphVersions(
      graph: Ptr[TF_Graph],
      output_version_def: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** TF_ImportGraphDefOptions holds options that can be passed to
   *  TF_GraphImportGraphDef.
   */
  type TF_ImportGraphDefOptions = CStruct0

  /** */
  def TF_NewImportGraphDefOptions(): Ptr[TF_ImportGraphDefOptions] = extern

  /** */
  def TF_DeleteImportGraphDefOptions(
      opts: Ptr[TF_ImportGraphDefOptions]
  ): Unit = extern

  /** Set the prefix to be prepended to the names of nodes in `graph_def` that
   *  will be imported into `graph`. `prefix` is copied and has no lifetime
   *  requirements.
   */
  def TF_ImportGraphDefOptionsSetPrefix(
      opts: Ptr[TF_ImportGraphDefOptions],
      prefix: CString
  ): Unit = extern

  /** Set the execution device for nodes in `graph_def`. Only applies to nodes
   *  where a device was not already explicitly specified. `device` is copied
   *  and has no lifetime requirements.
   */
  def TF_ImportGraphDefOptionsSetDefaultDevice(
      opts: Ptr[TF_ImportGraphDefOptions],
      device: CString
  ): Unit = extern

  /** Set whether to uniquify imported operation names. If true, imported
   *  operation names will be modified if their name already exists in the
   *  graph. If false, conflicting names will be treated as an error. Note that
   *  this option has no effect if a prefix is set, since the prefix will
   *  guarantee all names are unique. Defaults to false.
   */
  def TF_ImportGraphDefOptionsSetUniquifyNames(
      opts: Ptr[TF_ImportGraphDefOptions],
      uniquify_names: CUnsignedChar
  ): Unit = extern

  /** If true, the specified prefix will be modified if it already exists as an
   *  operation name or prefix in the graph. If false, a conflicting prefix will
   *  be treated as an error. This option has no effect if no prefix is
   *  specified.
   */
  def TF_ImportGraphDefOptionsSetUniquifyPrefix(
      opts: Ptr[TF_ImportGraphDefOptions],
      uniquify_prefix: CUnsignedChar
  ): Unit = extern

  /** Set any imported nodes with input `src_name:src_index` to have that input
   *  replaced with `dst`. `src_name` refers to a node in the graph to be
   *  imported, `dst` references a node already existing in the graph being
   *  imported into. `src_name` is copied and has no lifetime requirements.
   */
  @name("scalanative_TF_ImportGraphDefOptionsAddInputMapping")
  def TF_ImportGraphDefOptionsAddInputMapping(
      opts: Ptr[TF_ImportGraphDefOptions],
      src_name: CString,
      src_index: CInt,
      dst: Ptr[TF_Output]
  ): Unit = extern // TF_output

  /** Set any imported nodes with control input `src_name` to have that input
   *  replaced with `dst`. `src_name` refers to a node in the graph to be
   *  imported, `dst` references an operation already existing in the graph
   *  being imported into. `src_name` is copied and has no lifetime
   *  requirements.
   */
  def TF_ImportGraphDefOptionsRemapControlDependency(
      opts: Ptr[TF_ImportGraphDefOptions],
      src_name: CString,
      dst: Ptr[TF_Operation]
  ): Unit = extern

  /** Cause the imported graph to have a control dependency on `oper`. `oper`
   *  should exist in the graph being imported into.
   */
  def TF_ImportGraphDefOptionsAddControlDependency(
      opts: Ptr[TF_ImportGraphDefOptions],
      oper: Ptr[TF_Operation]
  ): Unit = extern

  /** Add an output in `graph_def` to be returned via the `return_outputs`
   *  output parameter of TF_GraphImportGraphDef(). If the output is remapped
   *  via an input mapping, the corresponding existing tensor in `graph` will be
   *  returned. `oper_name` is copied and has no lifetime requirements.
   */
  def TF_ImportGraphDefOptionsAddReturnOutput(
      opts: Ptr[TF_ImportGraphDefOptions],
      oper_name: CString,
      index: CInt
  ): Unit = extern

  /** Returns the number of return outputs added via
   *  TF_ImportGraphDefOptionsAddReturnOutput().
   */
  def TF_ImportGraphDefOptionsNumReturnOutputs(
      opts: Ptr[TF_ImportGraphDefOptions]
  ): CInt = extern

  /** Add an operation in `graph_def` to be returned via the `return_opers`
   *  output parameter of TF_GraphImportGraphDef(). `oper_name` is copied and
   *  has no lifetime requirements.
   */
  def TF_ImportGraphDefOptionsAddReturnOperation(
      opts: Ptr[TF_ImportGraphDefOptions],
      oper_name: CString
  ): Unit = extern

  /** Returns the number of return operations added via
   *  TF_ImportGraphDefOptionsAddReturnOperation().
   */
  def TF_ImportGraphDefOptionsNumReturnOperations(
      opts: Ptr[TF_ImportGraphDefOptions]
  ): CInt = extern

  /** TF_ImportGraphDefResults holds results that are generated by
   *  TF_GraphImportGraphDefWithResults().
   */
  type TF_ImportGraphDefResults = CStruct0

  /** Fetches the return outputs requested via
   *  TF_ImportGraphDefOptionsAddReturnOutput(). The number of fetched outputs
   *  is returned in `num_outputs`. The array of return outputs is returned in
   *  `outputs`. `*outputs` is owned by and has the lifetime of `results`.
   */
  def TF_ImportGraphDefResultsReturnOutputs(
      results: Ptr[TF_ImportGraphDefResults],
      num_outputs: Ptr[CInt],
      outputs: Ptr[Ptr[TF_Output]]
  ): Unit = extern

  /** Fetches the return operations requested via
   *  TF_ImportGraphDefOptionsAddReturnOperation(). The number of fetched
   *  operations is returned in `num_opers`. The array of return operations is
   *  returned in `opers`. `*opers` is owned by and has the lifetime of
   *  `results`.
   */
  def TF_ImportGraphDefResultsReturnOperations(
      results: Ptr[TF_ImportGraphDefResults],
      num_opers: Ptr[CInt],
      opers: Ptr[Ptr[Ptr[TF_Operation]]]
  ): Unit = extern

  /** Fetches any input mappings requested via
   *  TF_ImportGraphDefOptionsAddInputMapping() that didn't appear in the
   *  GraphDef and weren't used as input to any node in the imported graph def.
   *  The number of fetched mappings is returned in
   *  `num_missing_unused_input_mappings`. The array of each mapping's source
   *  node name is returned in `src_names`, and the array of each mapping's
   *  source index is returned in `src_indexes`.
   *
   *  `*src_names`, `*src_indexes`, and the memory backing each string in
   *  `src_names` are owned by and have the lifetime of `results`.
   */
  def TF_ImportGraphDefResultsMissingUnusedInputMappings(
      results: Ptr[TF_ImportGraphDefResults],
      num_missing_unused_input_mappings: Ptr[CInt],
      src_names: Ptr[Ptr[CString]],
      src_indexes: Ptr[Ptr[CInt]]
  ): Unit = extern

  /** Deletes a results object returned by TF_GraphImportGraphDefWithResults().
   */
  def TF_DeleteImportGraphDefResults(
      results: Ptr[TF_ImportGraphDefResults]
  ): Unit = extern

  /** Import the graph serialized in `graph_def` into `graph`. Returns nullptr
   *  and a bad status on error. Otherwise, returns a populated
   *  TF_ImportGraphDefResults instance. The returned instance must be deleted
   *  via TF_DeleteImportGraphDefResults().
   */
  def TF_GraphImportGraphDefWithResults(
      graph: Ptr[TF_Graph],
      graph_def: Ptr[TF_Buffer],
      options: Ptr[TF_ImportGraphDefOptions],
      status: Ptr[TF_Status]
  ): Ptr[TF_ImportGraphDefResults] = extern

  /** Import the graph serialized in `graph_def` into `graph`. Convenience
   *  function for when only return outputs are needed.
   *
   *  `num_return_outputs` must be the number of return outputs added (i.e. the
   *  result of TF_ImportGraphDefOptionsNumReturnOutputs()). If
   *  `num_return_outputs` is non-zero, `return_outputs` must be of length
   *  `num_return_outputs`. Otherwise it can be null.
   */
  def TF_GraphImportGraphDefWithReturnOutputs(
      graph: Ptr[TF_Graph],
      graph_def: Ptr[TF_Buffer],
      options: Ptr[TF_ImportGraphDefOptions],
      return_outputs: Ptr[TF_Output],
      num_return_outputs: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Import the graph serialized in `graph_def` into `graph`. Convenience
   *  function for when no results are needed.
   */
  def TF_GraphImportGraphDef(
      graph: Ptr[TF_Graph],
      graph_def: Ptr[TF_Buffer],
      options: Ptr[TF_ImportGraphDefOptions],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Adds a copy of function `func` and optionally its gradient function `grad`
   *  to `g`. Once `func`/`grad` is added to `g`, it can be called by creating
   *  an operation using the function's name. Any changes to `func`/`grad`
   *  (including deleting it) done after this method returns, won't affect the
   *  copy of `func`/`grad` in `g`. If `func` or `grad` are already in `g`,
   *  TF_GraphCopyFunction has no effect on them, but can establish the
   *  function->gradient relationship between them if `func` does not already
   *  have a gradient. If `func` already has a gradient different from `grad`,
   *  an error is returned.
   *
   *  `func` must not be null. If `grad` is null and `func` is not in `g`,
   *  `func` is added without a gradient. If `grad` is null and `func` is in
   *  `g`, TF_GraphCopyFunction is a noop. `grad` must have appropriate
   *  signature as described in the doc of GradientDef in
   *  tensorflow/core/framework/function.proto.
   *
   *  If successful, status is set to OK and `func` and `grad` are added to `g`.
   *  Otherwise, status is set to the encountered error and `g` is unmodified.
   */
  def TF_GraphCopyFunction(
      g: Ptr[TF_Graph],
      func: Ptr[TF_Function],
      grad: Ptr[TF_Function],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Returns the number of TF_Functions registered in `g`.
   */
  def TF_GraphNumFunctions(g: Ptr[TF_Graph]): CInt = extern

  /** Fills in `funcs` with the TF_Function* registered in `g`. `funcs` must
   *  point to an array of TF_Function* of length at least `max_func`. In usual
   *  usage, max_func should be set to the result of TF_GraphNumFunctions(g). In
   *  this case, all the functions registered in `g` will be returned. Else, an
   *  unspecified subset.
   *
   *  If successful, returns the number of TF_Function* successfully set in
   *  `funcs` and sets status to OK. The caller takes ownership of all the
   *  returned TF_Functions. They must be deleted with TF_DeleteFunction. On
   *  error, returns 0, sets status to the encountered error, and the contents
   *  of funcs will be undefined.
   */
  def TF_GraphGetFunctions(
      g: Ptr[TF_Graph],
      funcs: Ptr[Ptr[TF_Function]],
      max_func: CInt,
      status: Ptr[TF_Status]
  ): CInt = extern

  /** Note: The following function may fail on very large protos in the future.
   */
  def TF_OperationToNodeDef(
      oper: Ptr[TF_Operation],
      output_node_def: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Creates a TF_WhileParams for creating a while loop in `g`. `inputs` are
   *  outputs that already exist in `g` used as initial values for the loop
   *  variables.
   *
   *  The returned TF_WhileParams will have all fields initialized except
   *  `cond_output`, `body_outputs`, and `name`. The `body_outputs` buffer will
   *  be allocated to size `ninputs`. The caller should build `cond_graph` and
   *  `body_graph` starting from the inputs, and store the final outputs in
   *  `cond_output` and `body_outputs`.
   *
   *  If `status` is OK, the caller must call either TF_FinishWhile or
   *  TF_AbortWhile on the returned TF_WhileParams. If `status` isn't OK, the
   *  returned TF_WhileParams is not valid, and the caller should not call
   *  TF_FinishWhile() or TF_AbortWhile().
   *
   *  Missing functionality (TODO):
   *    - Gradients
   *    - Reference-type inputs
   *    - Directly referencing external tensors from the cond/body graphs (this
   *      is possible in the Python API)
   */
  def TF_NewWhile(
      g: Ptr[TF_Graph],
      inputs: Ptr[TF_Output],
      ninputs: CInt,
      status: Ptr[TF_Status]
  ): TF_WhileParams = extern

  /** Builds the while loop specified by `params` and returns the output tensors
   *  of the while loop in `outputs`. `outputs` should be allocated to size
   *  `params.ninputs`.
   *
   *  `params` is no longer valid once this returns.
   *
   *  Either this or TF_AbortWhile() must be called after a successful
   *  TF_NewWhile() call.
   */
  def TF_FinishWhile(
      params: Ptr[TF_WhileParams],
      status: Ptr[TF_Status],
      outputs: Ptr[TF_Output]
  ): Unit = extern

  /** Frees `params`s resources without building a while loop. `params` is no
   *  longer valid after this returns. Either this or TF_FinishWhile() must be
   *  called after a successful TF_NewWhile() call.
   */
  def TF_AbortWhile(params: Ptr[TF_WhileParams]): Unit = extern

  /** Adds operations to compute the partial derivatives of sum of `y`s w.r.t
   *  `x`s, i.e., d(y_1 + y_2 + ...)/dx_1, d(y_1 + y_2 + ...)/dx_2...
   *
   *  `dx` are used as initial gradients (which represent the symbolic partial
   *  derivatives of some loss function `L` w.r.t. `y`). `dx` must be nullptr or
   *  have size `ny`. If `dx` is nullptr, the implementation will use dx of
   *  `OnesLike` for all shapes in `y`. The partial derivatives are returned in
   *  `dy`. `dy` should be allocated to size `nx`.
   *
   *  Gradient nodes are automatically named under the "gradients/" prefix. To
   *  guarantee name uniqueness, subsequent calls to the same graph will append
   *  an incremental tag to the prefix: "gradients_1/", "gradients_2/", ... See
   *  TF_AddGradientsWithPrefix, which provides a means to specify a custom name
   *  prefix for operations added to a graph to compute the gradients.
   *
   *  WARNING: This function does not yet support all the gradients that python
   *  supports. See
   *  https://www.tensorflow.org/code/tensorflow/cc/gradients/README.md for
   *  instructions on how to add C++ more gradients.
   */
  def TF_AddGradients(
      g: Ptr[TF_Graph],
      y: Ptr[TF_Output],
      ny: CInt,
      x: Ptr[TF_Output],
      nx: CInt,
      dx: Ptr[TF_Output],
      status: Ptr[TF_Status],
      dy: Ptr[TF_Output]
  ): Unit = extern

  /** Adds operations to compute the partial derivatives of sum of `y`s w.r.t
   *  `x`s, i.e., d(y_1 + y_2 + ...)/dx_1, d(y_1 + y_2 + ...)/dx_2... This is a
   *  variant of TF_AddGradients that allows to caller to pass a custom name
   *  prefix to the operations added to a graph to compute the gradients.
   *
   *  `dx` are used as initial gradients (which represent the symbolic partial
   *  derivatives of some loss function `L` w.r.t. `y`). `dx` must be nullptr or
   *  have size `ny`. If `dx` is nullptr, the implementation will use dx of
   *  `OnesLike` for all shapes in `y`. The partial derivatives are returned in
   *  `dy`. `dy` should be allocated to size `nx`. `prefix` names the scope into
   *  which all gradients operations are being added. `prefix` must be unique
   *  within the provided graph otherwise this operation will fail. If `prefix`
   *  is nullptr, the default prefixing behaviour takes place, see
   *  TF_AddGradients for more details.
   *
   *  WARNING: This function does not yet support all the gradients that python
   *  supports. See
   *  https://www.tensorflow.org/code/tensorflow/cc/gradients/README.md for
   *  instructions on how to add C++ more gradients.
   */
  def TF_AddGradientsWithPrefix(
      g: Ptr[TF_Graph],
      prefix: CString,
      y: Ptr[TF_Output],
      ny: CInt,
      x: Ptr[TF_Output],
      nx: CInt,
      dx: Ptr[TF_Output],
      status: Ptr[TF_Status],
      dy: Ptr[TF_Output]
  ): Unit = extern

  /** Create a TF_Function from a TF_Graph
   *
   *  Params:
   *
   *  fn_body
   *    - the graph whose operations (or subset of whose operations) will be
   *      converted to TF_Function.
   *
   *  fn_name
   *    - the name of the new TF_Function. Should match the operation name
   *      (OpDef.name) regexp [A-Z][A-Za-z0-9_.\\-/]*. If
   *      `append_hash_to_fn_name` is false, `fn_name` must be distinct from
   *      other function and operation names (at least those registered in
   *      graphs where this function will be used).
   *
   *  append_hash_to_fn_name
   *    - Must be 0 or 1. If set to 1, the actual name of the function will be
   *      `fn_name` appended with '_<hash_of_this_function's_definition>'. If
   *      set to 0, the function's name will be `fn_name`.
   *
   *  num_opers
   *    - `num_opers` contains the number of elements in the `opers` array or a
   *      special value of -1 meaning that no array is given. The distinction
   *      between an empty array of operations and no array of operations is
   *      necessary to distinguish the case of creating a function with no body
   *      (e.g. identity or permutation) and the case of creating a function
   *      whose body contains all the nodes in the graph (except for the
   *      automatic skipping, see below).
   *
   *  opers
   *    - Array of operations to become the body of the function or null.
   *      - If no array is given (`num_opers` = -1), all the operations in
   *        `fn_body` will become part of the function except operations
   *        referenced in `inputs`. These operations must have a single output
   *        (these operations are typically placeholders created for the sole
   *        purpose of representing an input. We can relax this constraint if
   *        there are compelling use cases).
   *      - If an array is given (`num_opers` >= 0), all operations in it will
   *        become part of the function. In particular, no automatic skipping of
   *        dummy input operations is performed.
   *
   *  ninputs
   *    - number of elements in `inputs` array
   *
   *  inputs
   *    - array of TF_Outputs that specify the inputs to the function. If
   *      `ninputs` is zero (the function takes no inputs), `inputs` can be
   *      null. The names used for function inputs are normalized names of the
   *      operations (usually placeholders) pointed to by `inputs`. These
   *      operation names should start with a letter. Normalization will convert
   *      all letters to lowercase and non-alphanumeric characters to '_' to
   *      make resulting names match the "[a-z][a-z0-9_]*" pattern for operation
   *      argument names. `inputs` cannot contain the same tensor twice.
   *
   *  noutputs
   *    - number of elements in `outputs` array outputs - array of TF_Outputs
   *      that specify the outputs of the function. If `noutputs` is zero (the
   *      function returns no outputs), `outputs` can be null. `outputs` can
   *      contain the same tensor more than once.
   *
   *  output_names
   *    - The names of the function's outputs. `output_names` array must either
   *      have the same length as `outputs` (i.e. `noutputs`) or be null. In the
   *      former case, the names should match the regular expression for ArgDef
   *      names - "[a-z][a-z0-9_]*". In the latter case, names for outputs will
   *      be generated automatically.
   *
   *  opts
   *    - various options for the function, e.g. XLA's inlining control.
   *
   *  description
   *    - optional human-readable description of this function.
   *
   *  status
   *    - Set to OK on success and an appropriate error on failure.
   *
   *  Note that when the same TF_Output is listed as both an input and an
   *  output, the corresponding function's output will equal to this input,
   *  instead of the original node's output.
   *
   *  Callers must also satisfy the following constraints:
   *    - `inputs` cannot refer to TF_Outputs within a control flow context. For
   *      example, one cannot use the output of "switch" node as input.
   *    - `inputs` and `outputs` cannot have reference types. Reference types
   *      are not exposed through C API and are being replaced with Resources.
   *      We support reference types inside function's body to support legacy
   *      code. Do not use them in new code.
   *    - Every node in the function's body must have all of its inputs
   *      (including control inputs). In other words, for every node in the
   *      body, each input must be either listed in `inputs` or must come from
   *      another node in the body. In particular, it is an error to have a
   *      control edge going from a node outside of the body into a node in the
   *      body. This applies to control edges going from nodes referenced in
   *      `inputs` to nodes in the body when the former nodes are not in the
   *      body (automatically skipped or not included in explicitly specified
   *      body).
   *
   *  Returns: On success, a newly created TF_Function instance. It must be
   *  deleted by calling TF_DeleteFunction.
   */
  def TF_GraphToFunction(
      fn_body: Ptr[TF_Graph],
      fn_name: CString,
      append_hash_to_fn_name: CUnsignedChar,
      num_opers: CInt,
      opers: Ptr[Ptr[TF_Operation]],
      ninputs: CInt,
      inputs: Ptr[TF_Output],
      noutputs: CInt,
      outputs: Ptr[TF_Output],
      output_names: Ptr[CString],
      opts: Ptr[TF_FunctionOptions],
      description: CString,
      status: Ptr[TF_Status]
  ): Ptr[TF_Function] = extern

  /** Returns the name of the graph function. The return value points to memory
   *  that is only usable until the next mutation to *func.
   */
  def TF_FunctionName(func: Ptr[TF_Function]): CString = extern

  /** Write out a serialized representation of `func` (as a FunctionDef protocol
   *  message) to `output_func_def` (allocated by TF_NewBuffer()).
   *  `output_func_def`'s underlying buffer will be freed when TF_DeleteBuffer()
   *  is called.
   *
   *  May fail on very large graphs in the future.
   */
  def TF_FunctionToFunctionDef(
      func: Ptr[TF_Function],
      output_func_def: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Construct and return the function whose FunctionDef representation is
   *  serialized in `proto`. `proto_len` must equal the number of bytes pointed
   *  to by `proto`. Returns: On success, a newly created TF_Function instance.
   *  It must be deleted by calling TF_DeleteFunction.
   *
   *  On failure, null.
   */
  def TF_FunctionImportFunctionDef(
      proto: Ptr[Byte],
      proto_len: CSize,
      status: Ptr[TF_Status]
  ): Ptr[TF_Function] =
    extern

  /** Sets function attribute named `attr_name` to value stored in `proto`. If
   *  this attribute is already set to another value, it is overridden. `proto`
   *  should point to a sequence of bytes of length `proto_len` representing a
   *  binary serialization of an AttrValue protocol buffer.
   */
  def TF_FunctionSetAttrValueProto(
      func: Ptr[TF_Function],
      attr_name: CString,
      proto: Ptr[Byte],
      proto_len: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Sets `output_attr_value` to the binary-serialized AttrValue proto
   *  representation of the value of the `attr_name` attr of `func`. If
   *  `attr_name` attribute is not present, status is set to an error.
   */
  def TF_FunctionGetAttrValueProto(
      func: Ptr[TF_Function],
      attr_name: CString,
      output_attr_value: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Frees the memory used by the `func` struct. TF_DeleteFunction is a noop if
   *  `func` is null. Deleting a function does not remove it from any graphs it
   *  was copied to.
   */
  def TF_DeleteFunction(func: Ptr[TF_Function]): Unit = extern

  /** Attempts to evaluate `output`. This will only be possible if `output`
   *  doesn't depend on any graph inputs (this function is safe to call if this
   *  isn't the case though).
   *
   *  If the evaluation is successful, this function returns true and `output`s
   *  value is returned in `result`. Otherwise returns false. An error status is
   *  returned if something is wrong with the graph or input. Note that this may
   *  return false even if no error status is set.
   */
  @name("scalanative_TF_TryEvaluateConstant")
  def TF_TryEvaluateConstant(
      graph: Ptr[TF_Graph],
      output: Ptr[TF_Output], // TF_output
      result: Ptr[Ptr[TF_Tensor]],
      status: Ptr[TF_Status]
  ): CUnsignedChar = extern

  /** API for driving Graph execution.
   */
  type TF_Session = CStruct0

  /** Return a new execution session with the associated graph, or NULL on
   *  error. Does not take ownership of any input parameters.
   *
   *  *`graph` must be a valid graph (not deleted or nullptr). `graph` will be
   *  be kept alive for the lifetime of the returned TF_Session. New nodes can
   *  still be added to `graph` after this call.
   */
  def TF_NewSession(
      graph: Ptr[TF_Graph],
      opts: Ptr[TF_SessionOptions],
      status: Ptr[TF_Status]
  ): Ptr[TF_Session] = extern

  /** This function creates a new TF_Session (which is created on success) using
   *  `session_options`, and then initializes state (restoring tensors and other
   *  assets) using `run_options`.
   *
   *  Any NULL and non-NULL value combinations for (`run_options,
   *  `meta_graph_def`) are valid.
   *
   *    - `export_dir` must be set to the path of the exported SavedModel.
   *    - `tags` must include the set of tags used to identify one MetaGraphDef
   *      in the SavedModel.
   *    - `graph` must be a graph newly allocated with TF_NewGraph().
   *
   *  If successful, populates `graph` with the contents of the Graph and
   *  `meta_graph_def` with the MetaGraphDef of the loaded model.
   */
  def TF_LoadSessionFromSavedModel(
      session_options: Ptr[TF_SessionOptions],
      run_options: Ptr[TF_Buffer],
      export_dir: CString,
      tags: Ptr[CString],
      tags_len: CInt,
      graph: Ptr[TF_Graph],
      meta_graph_def: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Ptr[TF_Session] =
    extern

  /** Close a session.
   *
   *  Contacts any other processes associated with the session, if applicable.
   *  May not be called after TF_DeleteSession().
   */
  def TF_CloseSession(session: Ptr[TF_Session], status: Ptr[TF_Status]): Unit =
    extern

  /** Destroy a session object.
   *
   *  Even if error information is recorded in *status, this call discards all
   *  local resources associated with the session. The session may not be used
   *  during or after this call (and the session drops its reference to the
   *  corresponding graph).
   */
  def TF_DeleteSession(session: Ptr[TF_Session], status: Ptr[TF_Status]): Unit =
    extern

  /** Run the graph associated with the session starting with the supplied
   *  inputs (inputs[0,ninputs-1] with corresponding values in
   *  input_values[0,ninputs-1]).
   *
   *  Any NULL and non-NULL value combinations for (`run_options`,
   *  `run_metadata`) are valid.
   *
   *    - `run_options` may be NULL, in which case it will be ignored; or
   *      non-NULL, in which case it must point to a `TF_Buffer` containing the
   *      serialized representation of a `RunOptions` protocol buffer.
   *    - `run_metadata` may be NULL, in which case it will be ignored; or
   *      non-NULL, in which case it must point to an empty, freshly allocated
   *      `TF_Buffer` that may be updated to contain the serialized
   *      representation of a `RunMetadata` protocol buffer.
   *
   *  The caller retains ownership of `input_values` (which can be deleted using
   *  TF_DeleteTensor). The caller also retains ownership of `run_options`
   *  and/or `run_metadata` (when not NULL) and should manually call
   *  TF_DeleteBuffer on them.
   *
   *  On success, the tensors corresponding to outputs[0,noutputs-1] are placed
   *  in output_values[]. Ownership of the elements of output_values[] is
   *  transferred to the caller, which must eventually call TF_DeleteTensor on
   *  them.
   *
   *  On failure, output_values[] contains NULLs.
   */
  def TF_SessionRun(
      session: Ptr[TF_Session],
      //  RunOptions
      run_options: Ptr[TF_Buffer],
      // Input tensors
      inputs: Ptr[TF_Output],
      input_values: Ptr[Ptr[TF_Tensor]],
      ninputs: CInt,
      // Output tensors
      outputs: Ptr[TF_Output],
      output_values: Ptr[Ptr[TF_Tensor]],
      noutputs: CInt,
      // Target operations
      target_opers: Ptr[Ptr[TF_Operation]],
      ntargets: CInt,
      // RunMetadata
      run_metadata: Ptr[TF_Buffer],
      // Output status
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Set up the graph with the intended feeds (inputs) and fetches (outputs)
   *  for a sequence of partial run calls.
   *
   *  On success, returns a handle that is used for subsequent PRun calls. The
   *  handle should be deleted with TF_DeletePRunHandle when it is no longer
   *  needed.
   *
   *  On failure, out_status contains a tensorflow::Status with an error
   *  message. *handle is set to nullptr.
   */
  def TF_SessionPRunSetup(
      session: Ptr[TF_Session],
      // Input names
      inputs: Ptr[TF_Output],
      ninputs: CInt,
      // Output names
      outputs: Ptr[TF_Output],
      noutputs: CInt,
      // Target operations
      target_opers: Ptr[Ptr[TF_Operation]],
      ntargets: CInt,
      // Output handle
      handle: Ptr[CString],
      // Output status
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Continue to run the graph with additional feeds and fetches. The execution
   *  state is uniquely identified by the handle.
   */
  def TF_SessionPRun(
      session: Ptr[TF_Session],
      handle: CString,
      // Input tensors
      inputs: Ptr[TF_Output],
      input_values: Ptr[Ptr[TF_Tensor]],
      ninputs: CInt,
      // Output tensors
      outputs: Ptr[TF_Output],
      output_values: Ptr[Ptr[TF_Tensor]],
      noutputs: CInt,
      // Target operations
      target_opers: Ptr[Ptr[TF_Operation]],
      ntargets: CInt,
      // Output status
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Deletes a handle allocated by TF_SessionPRunSetup. Once called, no more
   *  calls to TF_SessionPRun should be made.
   */
  def TF_DeletePRunHandle(handle: CString): Unit = extern

  /** The deprecated session API. Please switch to the above instead of
   *  TF_ExtendGraph(). This deprecated API can be removed at any time without
   *  notice.
   */
  type TF_DeprecatedSession = CStruct0

  /** */
  def TF_NewDeprecatedSession(
      sessionOptions: Ptr[TF_SessionOptions],
      status: Ptr[TF_Status]
  ): Ptr[TF_DeprecatedSession] = extern

  /** */
  def TF_CloseDeprecatedSession(
      deprecatedSession: Ptr[TF_DeprecatedSession],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_DeleteDeprecatedSession(
      deprecatedSession: Ptr[TF_DeprecatedSession],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** */
  def TF_Reset(
      opt: Ptr[TF_SessionOptions],
      containers: Ptr[CString],
      ncontainers: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Treat the bytes proto[0,proto_len-1] as a serialized GraphDef and add the
   *  nodes in that GraphDef to the graph for the session.
   *
   *  Prefer use of TF_Session and TF_GraphImportGraphDef over this.
   */
  def TF_ExtendGraph(
      deprecatedSession: Ptr[TF_DeprecatedSession],
      proto: Ptr[Byte],
      proto_len: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** See TF_SessionRun() above.
   */
  def TF_Run(
      deprecatedSession: Ptr[TF_DeprecatedSession],
      run_options: Ptr[TF_Buffer],
      input_names: Ptr[CString],
      inputs: Ptr[Ptr[TF_Tensor]],
      ninputs: CInt,
      output_names: Ptr[CString],
      outputs: Ptr[Ptr[TF_Tensor]],
      noutputs: CInt,
      target_oper_names: Ptr[CString],
      ntargets: CInt,
      run_metadata: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** See TF_SessionPRunSetup() above.
   */
  def TF_PRunSetup(
      deprecatedSession: Ptr[TF_DeprecatedSession],
      input_names: Ptr[CString],
      ninputs: CInt,
      output_names: Ptr[CString],
      noutputs: CInt,
      target_oper_names: Ptr[CString],
      ntargets: CInt,
      handle: Ptr[CString],
      status: Ptr[TF_Status]
  ): Unit = extern

  /** See TF_SessionPRun above.
   */
  def TF_PRun(
      deprecatedSession: Ptr[TF_DeprecatedSession],
      handle: CString,
      input_names: Ptr[CString],
      inputs: Ptr[Ptr[TF_Tensor]],
      ninputs: CInt,
      output_names: Ptr[CString],
      outputs: Ptr[Ptr[TF_Tensor]],
      noutputs: CInt,
      target_oper_names: Ptr[CString],
      ntargets: CInt,
      status: Ptr[TF_Status]
  ): Unit = extern

  type TF_DeviceList = CStruct0

  /** Lists all devices in a TF_Session.
   *
   *  Caller takes ownership of the returned TF_DeviceList* which must
   *  eventually be freed with a call to TF_DeleteDeviceList.
   */
  def TF_SessionListDevices(
      session: Ptr[TF_Session],
      status: Ptr[TF_Status]
  ): Ptr[TF_DeviceList] = extern

  /** Lists all devices in a TF_Session.
   *
   *  Caller takes ownership of the returned TF_DeviceList* which must
   *  eventually be freed with a call to TF_DeleteDeviceList.
   */
  def TF_DeprecatedSessionListDevices(
      session: Ptr[TF_DeprecatedSession],
      status: Ptr[TF_Status]
  ): Ptr[TF_DeviceList] = extern

  /** Deallocates the device list.
   */
  def TF_DeleteDeviceList(list: Ptr[TF_DeviceList]): Unit = extern

  /** Counts the number of elements in the device list.
   */
  def TF_DeviceListCount(list: Ptr[TF_DeviceList]): CInt = extern

  /** Retrieves the full name of the device (e.g. /job:worker/replica:0/...) The
   *  return value will be a pointer to a null terminated string. The caller
   *  must not modify or delete the string. It will be deallocated upon a call
   *  to TF_DeleteDeviceList.
   *
   *  If index is out of bounds, an error code will be set in the status object,
   *  and a null pointer will be returned.
   */
  def TF_DeviceListName(
      list: Ptr[TF_DeviceList],
      index: CInt,
      status: Ptr[TF_Status]
  ): CString = extern

  /** Retrieves the type of the device at the given index.
   *
   *  The caller must not modify or delete the string. It will be deallocated
   *  upon a call to TF_DeleteDeviceList.
   *
   *  If index is out of bounds, an error code will be set in the status object,
   *  and a null pointer will be returned.
   */
  def TF_DeviceListType(
      list: Ptr[TF_DeviceList],
      index: CInt,
      status: Ptr[TF_Status]
  ): CString = extern

  /** Retrieve the amount of memory associated with a given device.
   *
   *  If index is out of bounds, an error code will be set in the status object,
   *  and -1 will be returned.
   */
  def TF_DeviceListMemoryBytes(
      list: Ptr[TF_DeviceList],
      index: CInt,
      status: Ptr[TF_Status]
  ): int64_t = extern

  /** Retrieve the incarnation number of a given device.
   *
   *  If index is out of bounds, an error code will be set in the status object,
   *  and 0 will be returned.
   */
  def TF_DeviceListIncarnation(
      list: Ptr[TF_DeviceList],
      index: CInt,
      status: Ptr[TF_Status]
  ): uint64_t = extern

  // Load plugins containing custom ops and kernels

  /** TF_Library holds information about dynamically loaded TensorFlow plugins.
   */
  type TF_Library = CStruct0

  /** Load the library specified by library_filename and register the ops and
   *  kernels present in that library.
   *
   *  Pass "library_filename" to a platform-specific mechanism for dynamically
   *  loading a library. The rules for determining the exact location of the
   *  library are platform-specific and are not documented here.
   *
   *  On success, place OK in status and return the newly created library
   *  handle. The caller owns the library handle.
   *
   *  On failure, place an error status in status and return NULL.
   */
  def TF_LoadLibrary(
      library_filename: CString,
      status: Ptr[TF_Status]
  ): Ptr[TF_Library] = extern

  /** Get the OpList of OpDefs defined in the library pointed by lib_handle.
   *
   *  Returns a TF_Buffer. The memory pointed to by the result is owned by
   *  lib_handle. The data in the buffer will be the serialized OpList proto for
   *  ops defined in the library.
   */
  def TF_GetOpList(lib_handle: Ptr[TF_Library]): TF_Buffer = extern

  /** Frees the memory associated with the library handle. Does NOT unload the
   *  library.
   */
  def TF_DeleteLibraryHandle(lib_handle: Ptr[TF_Library]): Unit = extern

  /** Get the OpList of all OpDefs defined in this address space. Returns a
   *  TF_Buffer, ownership of which is transferred to the caller (and can be
   *  freed using TF_DeleteBuffer).
   *
   *  The data in the buffer will be the serialized OpList proto for ops
   *  registered in this address space.
   */
  def TF_GetAllOpList(): Ptr[TF_Buffer] = extern

  /** TF_ApiDefMap encapsulates a collection of API definitions for an
   *  operation.
   *
   *  This object maps the name of a TensorFlow operation to a description of
   *  the API to generate for it, as defined by the ApiDef protocol buffer (
   *  https://www.tensorflow.org/code/tensorflow/core/framework/api_def.proto)
   *
   *  The ApiDef messages are typically used to generate convenience wrapper
   *  functions for TensorFlow operations in various language bindings.
   */
  type TF_ApiDefMap = CStruct0

  /** Creates a new TF_ApiDefMap instance.
   *
   *  Params:
   *
   *  op_list_buffer
   *    - TF_Buffer instance containing serialized OpList protocol buffer. (See
   *      https://www.tensorflow.org/code/tensorflow/core/framework/op_def.proto
   *      for the OpList proto definition).
   *
   *  status
   *    - Set to OK on success and an appropriate error on failure.
   */
  def TF_NewApiDefMap(
      op_list_buffer: Ptr[TF_Buffer],
      status: Ptr[TF_Status]
  ): Ptr[TF_ApiDefMap] = extern

  /** Deallocates a TF_ApiDefMap.
   */
  def TF_DeleteApiDefMap(apimap: Ptr[TF_ApiDefMap]): Unit = extern

  /** Add ApiDefs to the map.
   *
   *  `text` corresponds to a text representation of an ApiDefs protocol
   *  message.
   *  (https://www.tensorflow.org/code/tensorflow/core/framework/api_def.proto).
   *
   *  The provided ApiDefs will be merged with existing ones in the map, with
   *  precedence given to the newly added version in case of conflicts with
   *  previous calls to TF_ApiDefMapPut.
   */
  def TF_ApiDefMapPut(
      api_def_map: Ptr[TF_ApiDefMap],
      text: CString,
      text_len: CSize,
      status: Ptr[TF_Status]
  ): Unit = extern

  /** Returns a serialized ApiDef protocol buffer for the TensorFlow operation
   *  named `name`.
   */
  def TF_ApiDefMapGet(
      api_def_map: Ptr[TF_ApiDefMap],
      name: CString,
      name_len: CSize,
      status: Ptr[TF_Status]
  ): Ptr[TF_Buffer] = extern

  // Kernel definition information.

  /** Returns a serialized KernelList protocol buffer containing KernelDefs for
   *  all registered kernels.
   */
  def TF_GetAllRegisteredKernels(status: Ptr[TF_Status]): Ptr[TF_Buffer] =
    extern

  /** Returns a serialized KernelList protocol buffer containing KernelDefs for
   *  all kernels registered for the operation named `name`.
   */
  def TF_GetRegisteredKernelsForOp(
      name: CString,
      status: Ptr[TF_Status]
  ): Ptr[TF_Buffer] =
    extern

  /** In-process TensorFlow server functionality, for use in distributed
   *  training. A Server instance encapsulates a set of devices and a Session
   *  target that can participate in distributed training. A server belongs to a
   *  cluster (specified by a ClusterSpec), and corresponds to a particular task
   *  in a named job. The server can communicate with any other server in the
   *  same cluster.
   *
   *  In-process TensorFlow server.
   */
  type TF_Server = CStruct0

  /** Creates a new in-process TensorFlow server configured using a serialized
   *  ServerDef protocol buffer provided via `proto` and `proto_len`.
   *
   *  The server will not serve any requests until TF_ServerStart is invoked.
   *  The server will stop serving requests once TF_ServerStop or
   *  TF_DeleteServer is invoked.
   */
  def TF_NewServer(
      proto: Ptr[Byte],
      proto_len: CSize,
      status: Ptr[TF_Status]
  ): Ptr[TF_Server] = extern

  /** Starts an in-process TensorFlow server.
   */
  def TF_ServerStart(server: Ptr[TF_Server], status: Ptr[TF_Status]): Unit =
    extern

  /** Stops an in-process TensorFlow server.
   */
  def TF_ServerStop(server: Ptr[TF_Server], status: Ptr[TF_Status]): Unit =
    extern

  /** Blocks until the server has been successfully stopped (via TF_ServerStop
   *  or TF_ServerClose).
   */
  def TF_ServerJoin(server: Ptr[TF_Server], status: Ptr[TF_Status]): Unit =
    extern

  /** Returns the target string that can be provided to TF_SetTarget() to
   *  connect a TF_Session to `server`.
   *
   *  The returned string is valid only until TF_DeleteServer is invoked.
   */
  def TF_ServerTarget(server: Ptr[TF_Server]): CString = extern

  /** Destroy an in-process TensorFlow server, frees memory. If server is
   *  running it will be stopped and joined.
   */
  def TF_DeleteServer(server: Ptr[TF_Server]): Unit = extern
}

import tensorflow._

object tensorflowOps {

  implicit class TF_Buffer_ops(val p: Ptr[TF_Buffer]) extends AnyVal {
    def data: Ptr[Byte] = p._1
    def data_=(value: Ptr[Byte]): Unit = p._1 = value
    def length: CSize = p._2
    def length_=(value: CSize): Unit = p._2 = value
    def data_deallocator: CFuncPtr2[Ptr[Byte], CSize, Unit] = p._3
    def data_deallocator_=(value: CFuncPtr2[Ptr[Byte], CSize, Unit]): Unit =
      p._3 = value
  }

  def TF_Buffer()(implicit z: Zone): Ptr[TF_Buffer] =
    alloc[TF_Buffer]()

  implicit class TF_Input_ops(val p: Ptr[TF_Input]) extends AnyVal {
    def oper: Ptr[TF_Operation] = p._1
    def oper_=(value: Ptr[TF_Operation]): Unit = p._1 = value
    def index: CInt = p._2
    def index_=(value: CInt): Unit = p._2 = value
  }

  def TF_Input()(implicit z: Zone): Ptr[TF_Input] =
    alloc[TF_Input]()

  implicit class TF_Output_ops(val p: Ptr[TF_Output]) extends AnyVal {
    def oper: Ptr[TF_Operation] = p._1
    def oper_=(value: Ptr[TF_Operation]): Unit = p._1 = value
    def index: CInt = p._2
    def index_=(value: CInt): Unit = p._2 = value
  }

  def TF_Output()(implicit z: Zone): Ptr[TF_Output] =
    alloc[TF_Output]()

  implicit class TF_AttrMetadata_ops(val p: Ptr[TF_AttrMetadata])
      extends AnyVal {
    def is_list: CUnsignedChar = p._1
    def is_list_=(value: CUnsignedChar): Unit = p._1 = value
    def list_size: int64_t = p._2
    def list_size_=(value: int64_t): Unit = p._2 = value
    def `type`: TF_AttrType = p._3
    def `type_=`(value: TF_AttrType): Unit = p._3 = value
    def total_size: int64_t = p._4
    def total_size_=(value: int64_t): Unit = p._4 = value
  }

  def TF_AttrMetadata()(implicit z: Zone): Ptr[TF_AttrMetadata] =
    alloc[TF_AttrMetadata]()

  implicit class TF_WhileParams_ops(val p: Ptr[TF_WhileParams]) extends AnyVal {
    def ninputs: CInt = p._1
    def ninputs_=(value: CInt): Unit = p._1 = value
    def cond_graph: Ptr[TF_Graph] = p._2
    def cond_graph_=(value: Ptr[TF_Graph]): Unit = p._2 = value
    def cond_inputs: Ptr[TF_Output] = p._3
    def cond_inputs_=(value: Ptr[TF_Output]): Unit = p._3 = value
    def cond_output: Ptr[TF_Output] = p._4 // TF_output
    def cond_output_=(value: Ptr[TF_Output]): Unit = p._4 = value // TF_output
    def body_graph: Ptr[TF_Graph] = p._5
    def body_graph_=(value: Ptr[TF_Graph]): Unit = p._5 = value
    def body_inputs: Ptr[TF_Output] = p._6
    def body_inputs_=(value: Ptr[TF_Output]): Unit = p._6 = value
    def body_outputs: Ptr[TF_Output] = p._7
    def body_outputs_=(value: Ptr[TF_Output]): Unit = p._7 = value
    def name: CString = p._8
    def name_=(value: CString): Unit = p._8 = value
  }

  def TF_WhileParams()(implicit z: Zone): Ptr[TF_WhileParams] =
    alloc[TF_WhileParams]()
}
