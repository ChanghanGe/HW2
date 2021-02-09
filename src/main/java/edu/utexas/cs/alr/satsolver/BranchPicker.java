package edu.utexas.cs.alr.satsolver;

public interface BranchPicker extends GenericListener {
    VariableValue select(Assignment assignment);
}