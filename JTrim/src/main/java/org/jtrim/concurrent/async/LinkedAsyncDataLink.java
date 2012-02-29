package org.jtrim.concurrent.async;

import java.util.LinkedList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncDatas#convertResult(AsyncDataLink, AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
final class LinkedAsyncDataLink<DataType> implements AsyncDataLink<DataType> {
    private final LinkAndConverter<?, DataType> linkAndConverter;

    public <SourceDataType> LinkedAsyncDataLink(
            AsyncDataLink<? extends SourceDataType> input,
            AsyncDataQuery<? super SourceDataType, ? extends DataType> converter) {
        this.linkAndConverter = new LinkAndConverter<>(input, converter);
    }

    private static void addStatesToList(AsyncDataState state,
            List<AsyncDataState> result) {

        if (state instanceof MultiAsyncDataState) {
            MultiAsyncDataState multiState = (MultiAsyncDataState)state;
            for (AsyncDataState subState: multiState.getSubStates()) {
                addStatesToList(subState, result);
            }
        }
        else {
            result.add(state);
        }
    }

    @Override
    public AsyncDataController getData(AsyncDataListener<? super DataType> dataListener) {
        return linkAndConverter.getData(dataListener);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
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

            List<AsyncDataState> states = new LinkedList<>();
            addStatesToList(mainState, states);
            addStatesToList(secondaryState, states);

            return new MultiAsyncDataState(states);
        }

        @Override
        public void cancel() {
            queryListener.cancel();
            inputController.cancel();
        }

        @Override
        public void controlData(Object controlArg) {
            if (controlArg instanceof LinkedDataControl) {
                LinkedDataControl linkedControl
                    = (LinkedDataControl)controlArg;

                Object mainControlData = linkedControl.getMainControlData();
                Object secControlData = linkedControl.getSecondaryControlData();

                if (mainControlData != null) {
                    inputController.controlData(mainControlData);
                }

                if (secControlData != null) {
                    queryListener.controlData(secControlData);
                }
            }
            else {
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

            ExceptionHelper.checkNotNullArgument(input, "input");
            ExceptionHelper.checkNotNullArgument(converter, "converter");

            this.input = input;
            this.converter = converter;
        }

        public AsyncDataQuery<? super SourceDataType, ? extends DataType> getConverter() {
            return converter;
        }

        public AsyncDataLink<? extends SourceDataType> getInput() {
            return input;
        }

        public AsyncDataController getData(AsyncDataListener<? super DataType> dataListener) {
            ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

            LinkedAsyncDataListener<SourceDataType> queryListener =
                    new LinkedAsyncDataListener<>(null, converter, dataListener);

            AsyncDataController inputController;
            inputController = input.getData(queryListener);

            return new LinkedController(inputController, queryListener);
        }
    }
}
