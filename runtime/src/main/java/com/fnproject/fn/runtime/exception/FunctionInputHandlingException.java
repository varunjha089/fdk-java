package com.fnproject.fn.runtime.exception;

/**
 * This used to wrap any exception thrown by an {@link com.fnproject.fn.api.InputCoercion}. It is
 * also thrown if no {@link com.fnproject.fn.api.InputCoercion} is applicable to a parameter of the user function.
 */
public class FunctionInputHandlingException extends RuntimeException {
    public FunctionInputHandlingException(String s, Throwable t) {
        super(s,t);
    }

    public FunctionInputHandlingException(String s) {
        super(s);

    }
}
