package org.jtrim.concurrent.async;

import java.util.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncDatas#convertResult(org.jtrim.concurrent.async.AsyncDataLink, org.jtrim.concurrent.async.AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
final class LinkedAsyncDataLink<DataType>
        implements AsyncDataLink<DataType> {

    private final AsyncDataLink<?> input;
    private final AsyncDataQuery<Object, ? extends DataType> converter;

    public <SourceDataType> LinkedAsyncDataLink(
            AsyncDataLink<? extends SourceDataType> input,
            AsyncDataQuery<? super SourceDataType, ? extends DataType> converter) {
        // Due to the constraint in the argument list the conversion from the
        // to the output is always valid.
        @SuppressWarnings("unchecked")
        AsyncDataQuery<Object, ? extends DataType> convertedConverter
                = (AsyncDataQuery<Object, ? extends DataType>)converter;

        this.input = input;
        this.converter = convertedConverter;
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
        ExceptionHelper.checkNotNullArgument(dataListener, "refType");

        LinkedAsyncDataListener<Object> queryListener;
        queryListener = new LinkedAsyncDataListener<>(
                null, converter, dataListener);

        AsyncDataController inputController;
        inputController = input.getData(queryListener);

        return new LinkedController(inputController, queryListener);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(input, result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(converter, result);

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
}
