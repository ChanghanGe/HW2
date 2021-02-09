package edu.utexas.cs.alr.satsolver;

public interface UnitPropagator extends GenericListener{
    boolean propagate(Formula formula, Assignment assignment);
}