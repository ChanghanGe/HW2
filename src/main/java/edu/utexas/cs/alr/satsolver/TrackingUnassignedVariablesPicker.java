package edu.utexas.cs.alr.satsolver;

import java.util.HashSet;
import java.util.Set;

public abstract class TrackingUnassignedVariablesPicker implements BranchPicker, Assignment.Listener {

    final Set<Integer> unassignedVariables = new HashSet<>();

    @Override
    public void init(Formula formula, Assignment assignment) {
        int variableCount = formula.getVariableCount();

        unassignedVariables.clear();
        for (int i = 1; i <= variableCount; i++) {
            if (!assignment.contains(i)) unassignedVariables.add(i);
        }
    }

    @Override
    public void add(int variable, boolean value, int antecedent) {
        unassignedVariables.remove(variable);
    }

    @Override
    public void remove(int variable, boolean value) {
        unassignedVariables.add(variable);
    }
}
