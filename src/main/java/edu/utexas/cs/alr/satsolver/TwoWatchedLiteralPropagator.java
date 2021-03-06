package edu.utexas.cs.alr.satsolver;

import java.util.*;

public class TwoWatchedLiteralPropagator extends Loggable
        implements UnitPropagator, Assignment.Listener, Formula.Listener {

    // Stores the two literals being watched of a clause
    private class LiteralPair {
        int first;
        int second;

        LiteralPair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        // Replace the slot that contains oldValue with newValue
        void replace(int oldValue, int newValue) {
            if (first == oldValue) first = newValue;
            else if (second == oldValue) second = newValue;
        }

        @Override
        public String toString() {
            return String.format("<%d, %d>", first, second);
        }
    }

    // Map of literal -> clauses that are watching that literals
    private Map<Integer, List<Integer>> watchLists;

    // Map of clause id -> watch literals
    private Map<Integer, LiteralPair> watchedPairs;

    // Queue of literals to propagate that have recently been set to FALSE
    private LinkedList<Integer> literalsToPropagate;

    // Mark the learned clause to be analyzed in propagate()
    private Clause recentlyLearnedClause = null;

    @Override
    public void init(Formula formula, Assignment assignment) {
        // decision level must be zero at the beginning
        assert (assignment.getCurrentDecisionLevel() == 0);

        watchLists = new HashMap<>();
        watchedPairs = new HashMap<>();
        literalsToPropagate = new LinkedList<>();

        // First, we watch all clauses with >= 2 literals
        formula.forEach(clause -> {
            if (clause.getLiteralSize() >= 2) {
                // if not a unit clause, we watch its first two literals
                watchNewClause(
                        clause.getId(),
                        clause.get(0).toLiteralNum(),
                        clause.get(1).toLiteralNum()
                );
            }
        });

        // Then, attempt to find and assign all unit clause.
        formula.forEach(clause -> {
            if (clause.getLiteralSize() == 1) {
                Literal unit = clause.get(0);
                assignment.add(unit.variable, !unit.isNegated, clause.getId());
            }
        });

        if (debug) Logger.debug("Initial watched pairs: ", watchedPairs);
    }

    private List<Integer> getWatchList(int literal) {
        return watchLists.computeIfAbsent(literal, k -> new ArrayList<>());
    }

    /**
     * Given the new learned clause's id, and two of its literals, updates the watch lists and watched pairs.
     */
    private void watchNewClause(int clauseId, int firstLiteral, int secondLiteral) {
        getWatchList(firstLiteral).add(clauseId);
        getWatchList(secondLiteral).add(clauseId);
        watchedPairs.put(clauseId, new LiteralPair(firstLiteral, secondLiteral));
    }


    // Given a clause and a watched literal, return the other watched literal
    private int findOtherWatchedLiteral(int clauseId, int literal) {
        LiteralPair watching = watchedPairs.get(clauseId);
        return literal != watching.first ? watching.first : watching.second;
    }

    @Override
    public void learn(Clause clause) {
        // a new clause is added to the formula. We'll just set the first two literals to be watched, regardless of
        // their values.

        recentlyLearnedClause = clause;
        if (debug) Logger.debug("New watched pairs after learning:", watchedPairs);
    }

    @Override
    public void add(int variable, boolean value, int antecedent) {
        // When a literal L becomes true, the solver needs to iterate only through the watch list for −L.
        // adds -L to our literal queue to be propagated later in propagate().
        literalsToPropagate.push(value ? -variable : variable);
    }

    /**
     * Processes the newly learned clause:
     * <p>
     * - If it's an unit clause, propagate the literal immediately.
     * <p>
     * If it's non-unit, watch two of its literals.
     */
    private void processLearnedClause(Clause learnedClause, Assignment assignment) {

        // we check if there is any recently learned clause
        if (learnedClause == null) return;

        // if it's a unit clause, just assign it right away.
        if (learnedClause.getLiteralSize() == 1) {
            Literal unitLiteral = learnedClause.get(0);
            assignment.add(
                    unitLiteral.variable, !unitLiteral.isNegated, learnedClause.getId(), 0
            );

            return;
        }

        int firstLiteral = learnedClause.get(0).toLiteralNum();
        int secondLiteral = learnedClause.get(1).toLiteralNum();

        watchNewClause(learnedClause.getId(), firstLiteral, secondLiteral);

        if (assignment.getLiteralValue(firstLiteral) == Logic.FALSE) literalsToPropagate.add(firstLiteral);
        if (assignment.getLiteralValue(secondLiteral) == Logic.FALSE) literalsToPropagate.add(secondLiteral);
    }

    @Override
    public boolean propagate(Formula formula, Assignment assignment) {

        // we check if there is any recently learned clause
        if (recentlyLearnedClause != null) {
            processLearnedClause(recentlyLearnedClause, assignment);
            recentlyLearnedClause = null;
        }

        // Loop until our queue is empty
        while (!literalsToPropagate.isEmpty()) {
            int falseLiteral = literalsToPropagate.pop();

            if (!watchLists.containsKey(falseLiteral)) continue;

            List<Integer> watchList = watchLists.get(falseLiteral);

            if (debug) Logger.debug(
                    "Considering falseLiteral:", falseLiteral,
                    ", watchList =", watchList
            );

            // iterate the watch list for -L
            ListIterator<Integer> clauseCandidates = watchList.listIterator();

            while (clauseCandidates.hasNext()) {
                int clauseId = clauseCandidates.next();

                int otherLiteralNum = findOtherWatchedLiteral(clauseId, falseLiteral);
                Literal otherLiteral = new Literal(otherLiteralNum);
                Logic otherLiteralValue = assignment.getLiteralValue(otherLiteral);

                // 1. If the other watched literal is true, do nothing.
                if (otherLiteralValue == Logic.TRUE) continue;

                // 2. If one of the unwatched literals L' is not false, restore
                // the invariant by updating the clause so that it watches L'
                // instead of −L.
                Clause currentClause = formula.getClause(clauseId);
                boolean hasUpdatedWatch = false;

                for (Literal unwatched : currentClause) {
                    int unwatchedNum = unwatched.toLiteralNum();
                    // if this literal is truly unwatched and is NOT false
                    if (unwatchedNum != falseLiteral && unwatchedNum != otherLiteralNum
                            && assignment.getLiteralValue(unwatchedNum) != Logic.FALSE) {

                        hasUpdatedWatch = true;

                        LiteralPair pair = watchedPairs.get(clauseId);
                        String oldPair = "";
                        if (debug) oldPair = pair.toString();

                        // replace falseLiteral with unwatchedNum
                        pair.replace(falseLiteral, unwatchedNum);

                        if (debug) Logger.debug((
                                String.format("clause %d, pair = %s => %s", clauseId, oldPair, pair)
                        ));

                        // remove the clause from the watchList for falseLiteral
                        clauseCandidates.remove();

                        // add the clause to unwatchedNum's new watchList
                        getWatchList(unwatchedNum).add(clauseId);
                        break; // we're done for this clause
                    }
                }

                // 3. Otherwise, consider the other watched literal L' in the clause:
                if (!hasUpdatedWatch) {
                    if (otherLiteralValue == Logic.UNDEFINED) {
                        // 3.1. If it is not set, propagate L′
                        Logger.debug("Propagate:", otherLiteral, "from clause", clauseId);
                        assignment.add(otherLiteral.variable, !otherLiteral.isNegated, clauseId);
                    } else {
                        // 3.2. Otherwise, L' is false, and we have found a conflict.
                        Logger.debug("Conflict at clause", clauseId);
                        assignment.setKappaAntecedent(clauseId);
                        literalsToPropagate.clear();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public TwoWatchedLiteralPropagator debug() {
        return (TwoWatchedLiteralPropagator) super.debug();
    }
}
