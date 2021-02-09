package edu.utexas.cs.alr.satsolver;


public interface GenericListener {
    /**
     * Initializes with the formula and the current assignment.
     */
    default void init(Formula formula, Assignment assignment) {
    }
}