package edu.utexas.cs.alr.satsolver;

public class VariableValue {
    public final int variable;
    public final boolean value;

    VariableValue(int variable, boolean value) {
        assert (variable > 0);
        this.variable = variable;
        this.value = value;
    }

    @Override
    public String toString() {
        return "x" + String.valueOf(variable) + " -> " + String.valueOf(value);
    }
}
