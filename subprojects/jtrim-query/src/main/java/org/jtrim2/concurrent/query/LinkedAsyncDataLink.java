package org.jtrim2.concurrent.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;

/**
 * @see AsyncLinks#convertResult(AsyncDataLink, AsyncDataQuery)
 */
final class LinkedAsyncDataLink<DataType> implements AsyncDataLink<DataType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final LinkAndConverter<?, DataType> linkAndConverter;

    public <SourceDataType> LinkedAsyncDataLink(
            AsyncDataLink<? extends SourceDataType> input,
            AsyncDataQuery<? super SourceDataType, ? extends DataType> converter) {
        this.linkAndConverter = new LinkAndConverter<>(input, converter);
    }

    private static void addStatesToList(AsyncDataState state,
            List<AsyncDataState> result) {

        if (state instanceof MultiAsyncDataState) {
            MultiAsyncDataState multiState = (MultiAsyncDataState) state;
            for (AsyncDataState subState: multiState.getSubStates()) {
                addStatesToList(subState, result);
            }
        } else {
            result.add(state);
        }
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super DataType> dataListener) {
        return linkAndConverter.getData(cancelToken, dataListener);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(linkAndConverter.getInput(), result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(linkAndConverter.getConverter(), result);

        return result.toString();
    }

    private static class LinkedController implements AsyncDataController {
        private final AsyncDataController inputController;
        private final LinkedAsyncDataListener<?> queryListener;

        public LinkedController(AsyncDataController inputController,
                LinkedAsyncDataListener<?> queryListener) {

            this.inputController = inputController;
            this.queryListener = queryListener;
        }

        @Override
        public AsyncDataState getDataState() {
            AsyncDataState mainState = inputController.getDataState();
            AsyncDataState secondaryState = queryListener.getDataState();

            List<AsyncDataState> states = new ArrayList<>();
            addStatesToList(mainState, states);
            addStatesToList(secondaryState, states);

            return new MultiAsyncDataState(states);
        }

        @Override
        public void controlData(Object controlArg) {
            if (controlArg instanceof LinkedDataControl) {
                LinkedDataControl linkedControl = (LinkedDataControl) controlArg;

                Object mainControlData = linkedControl.getMainControlData();
                Object secControlData = linkedControl.getSecondaryControlData();

                if (mainControlData != null) {
                    inputController.controlData(mainControlData);
                }

                if (secControlData != null) {
                    queryListener.controlData(secControlData);
                }
            } else {
                inputController.controlData(controlArg);
            }
        }
    }

    private static class LinkAndConverter<SourceDataType, DataType> {
        private final AsyncDataLink<? extends SourceDataType> input;
        private final AsyncDataQuery<? super SourceDataType, ? extends DataType> converter;

        public LinkAndConverter(
                AsyncDataLink<? extends SourceDataType> input,
                AsyncDataQuery<? super SourceDataType, ? extends DataType> converter) {

            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(converter, "converter");

            this.input = input;
            this.converter = converter;
        }

        public AsyncDataQuery<? super SourceDataType, ? extends DataType> getConverter() {
            return converter;
        }

        public AsyncDataLink<? extends SourceDataType> getInput() {
            return input;
        }

        public AsyncDataController getData(
                CancellationToken cancelToken,
                AsyncDataListener<? super DataType> dataListener) {

            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(dataListener, "dataListener");

            LinkedAsyncDataListener<SourceDataType> queryListener =
                    new LinkedAsyncDataListener<>(cancelToken, null, converter, dataListener);

            AsyncDataController inputController;
            inputController = input.getData(cancelToken, queryListener);

            return new LinkedController(inputController, queryListener);
        }
    }
}
