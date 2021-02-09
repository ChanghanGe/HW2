package edu.utexas.cs.alr.satsolver;

public interface SatSolver {
    /**
     * Solve a SAT formula.
     *
     * @param formula the formula to be solved.
     * @return an {@link Assignment} if the formula is satisfiable, or null otherwise.
     */
    Assignment solve(Formula formula);
}