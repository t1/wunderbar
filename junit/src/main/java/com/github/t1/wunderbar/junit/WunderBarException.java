package com.github.t1.wunderbar.junit;

/** Something was wrong with the test code setup */
public class WunderBarException extends IllegalArgumentException {
    public WunderBarException(String message) {super(message);}

    public WunderBarException(String message, Throwable cause) {super(message, cause);}
}
