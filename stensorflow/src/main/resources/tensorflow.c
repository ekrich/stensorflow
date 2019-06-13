
/*
 * Converter functions for TF_Input and TF_Output pass by reference
 */

#include <tensorflow.h>

TF_Output *scalanative_TF_OperationInput(TF_Input *oper_in) {
  return &TF_OperationInput(*oper_in);
}

int scalanative_TF_OperationOutputNumConsumers(TF_Output *oper_out) {
  return TF_OperationOutputNumConsumers(*oper_out);
}

int scalanative_TF_OperationOutputConsumers(TF_Output *oper_out,
                                            TF_Input *consumers,
                                            int max_consumers) {
  return TF_OperationOutputConsumers(*oper_out, consumers, max_consumers);
}

void scalanative_TF_ImportGraphDefOptionsAddInputMapping(
    TF_ImportGraphDefOptions *opts, char *src_name, int src_index,
    TF_Output *dst) {
  return TF_ImportGraphDefOptionsAddInputMapping(opts, src_name, src_index,
                                                 *dst);
}

unsigned char scalanative_TF_TryEvaluateConstant(TF_Graph *graph,
                                                 TF_Output *output,
                                                 TF_Tensor **result,
                                                 TF_Status *status) {
  return TF_TryEvaluateConstant(graph, *output, result, status);
}

void scalanative_TF_GraphSetTensorShape(TF_Graph *graph, TF_Output *output,
                                        int64_t *dims, int num_dims,
                                        TF_Status *status) {
  return TF_GraphSetTensorShape(graph, *output, dims, num_dims, status);
}

int scalanative_TF_GraphGetTensorNumDims(TF_Graph *graph, TF_Output *output,
                                         TF_Status *status) {
  return TF_GraphGetTensorNumDims(graph, *output, status);
}

void scalanative_TF_GraphGetTensorShape(TF_Graph *graph, TF_Output *output,
                                        int64_t *dims, int num_dims,
                                        TF_Status status) {
  return TF_GraphGetTensorShape(graph, *output, dims, num_dims, status);
}

void scalanative_TF_AddInput(TF_OperationDescription *desc, TF_Output *input) {
  return TF_AddInput(desc, *input);
}

TF_DataType scalanative_TF_OperationOutputType(TF_Output *oper_out) {
  return TF_OperationOutputType(*oper_out);
}