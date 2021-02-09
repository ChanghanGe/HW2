package edu.utexas.cs.alr.satsolver;

public class HybridVsidsPicker extends RandomPicker implements Formula.Listener {
    private VsidsPicker vsidsPicker = new VsidsPicker();

    // Default probability for random choices: 10%.
    private float randomPickingRatio = 0.1f;

    public HybridVsidsPicker() {
    }

    public HybridVsidsPicker(float randomPickingRatio) {
        this.randomPickingRatio = randomPickingRatio;
    }

    @Override
    public VariableValue select(Assignment assignment) {
        if (rand.nextFloat() <= randomPickingRatio) {
            return super.select(assignment);
        } else {
            return vsidsPicker.select(assignment);
        }
    }

    @Override
    public void init(Formula formula, Assignment assignment) {
        super.init(formula, assignment);
        vsidsPicker.init(formula, assignment);
    }

    @Override
    public void learn(Clause learnedClause) {
        vsidsPicker.learn(learnedClause);
    }
}
