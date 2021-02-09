package edu.utexas.cs.alr.satsolver;

import java.util.Random;

public class RandomPicker extends TrackingUnassignedVariablesPicker {
    protected final Random rand = new Random();

    public RandomPicker() {
        rand.setSeed(System.currentTimeMillis());
    }

    @Override
    public VariableValue select(Assignment assignment) {
        int randomIndex = rand.nextInt(unassignedVariables.size());
        int variable = unassignedVariables.stream().skip(randomIndex).findFirst().get();
        return new VariableValue(variable, rand.nextBoolean());
    }
}