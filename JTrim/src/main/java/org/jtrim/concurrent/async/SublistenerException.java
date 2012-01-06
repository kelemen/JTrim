/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public class SublistenerException extends RuntimeException {
    private static final long serialVersionUID = -2529316001636841487L;

    public SublistenerException(Throwable cause) {
        super(cause);
    }

    public SublistenerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SublistenerException(String message) {
        super(message);
    }

    public SublistenerException() {
    }
}
