/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.operators.python.process;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.python.DataStreamPythonFunctionInfo;
import org.apache.flink.streaming.api.operators.BoundedMultiInput;
import org.apache.flink.streaming.api.operators.InputSelectable;
import org.apache.flink.streaming.api.operators.InputSelection;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

/**
 * The {@link ExternalPythonBatchKeyedCoBroadcastProcessOperator} is responsible for executing the
 * Python CoBroadcastProcess function under BATCH mode, {@link ExternalPythonKeyedCoProcessOperator}
 * is used under STREAMING mode. This operator forces to run out data from broadcast side first, and
 * then process data from regular side.
 *
 * @param <OUT> The output type of the CoBroadcastProcess function
 */
@Internal
public class ExternalPythonBatchKeyedCoBroadcastProcessOperator<OUT>
        extends ExternalPythonKeyedCoProcessOperator<OUT>
        implements BoundedMultiInput, InputSelectable {

    private static final long serialVersionUID = 1L;

    private transient volatile boolean isBroadcastSideDone = false;

    public ExternalPythonBatchKeyedCoBroadcastProcessOperator(
            Configuration config,
            DataStreamPythonFunctionInfo pythonFunctionInfo,
            TypeInformation<Row> inputTypeInfo1,
            TypeInformation<Row> inputTypeInfo2,
            TypeInformation<OUT> outputTypeInfo) {
        super(config, pythonFunctionInfo, inputTypeInfo1, inputTypeInfo2, outputTypeInfo);
    }

    @Override
    public void endInput(int inputId) throws Exception {
        if (inputId == 2) {
            isBroadcastSideDone = true;
        }
    }

    @Override
    public InputSelection nextSelection() {
        if (!isBroadcastSideDone) {
            return InputSelection.SECOND;
        } else {
            return InputSelection.FIRST;
        }
    }

    @Override
    public void processElement1(StreamRecord<Row> element) throws Exception {
        Preconditions.checkState(
                isBroadcastSideDone,
                "Should not process regular input before broadcast side is done.");

        super.processElement1(element);
    }
}
