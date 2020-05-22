package es.urjc.etsii.grafo.solver.algorithms;

import es.urjc.etsii.grafo.io.Instance;
import es.urjc.etsii.grafo.solution.Solution;
import es.urjc.etsii.grafo.solver.create.Constructive;
import es.urjc.etsii.grafo.solver.create.SolutionBuilder;
import es.urjc.etsii.grafo.solver.destructor.Shake;
import es.urjc.etsii.grafo.solver.improve.Improver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class VNS<S extends Solution<I>, I extends Instance> extends BaseAlgorithm<S, I> {

    private static final Logger log = Logger.getLogger(VNS.class.getName());

    List<Improver<S, I>> improvers;
    Constructive<S, I> constructive;
    private final List<Shake<S, I>> shakes;
    private final int[] ks;
    private final int maxK;



    /**
     * Execute VNS until finished
     * @param ks Integer array that will be used for the shake/perturbation
     * @param builder How to build a Solution from the given Instance
     * @param shake Perturbation method
     * @param constructive Constructive method
     * @param improvers List of improvers/local searches
     */
    @SafeVarargs
    public VNS(int[] ks, SolutionBuilder<S, I> builder, Shake<S, I> shake, Constructive<S, I> constructive, Improver<S, I>... improvers) {
        this(ks, builder, Collections.singletonList(shake), constructive, improvers);
    }


    /**
     * Execute VNS until finished
     * @param ks Integer array that will be used for the shake/perturbation
     * @param builder How to build a Solution from the given Instance
     * @param shakes Perturbation method
     * @param constructive Constructive method
     * @param improvers List of improvers/local searches
     */
    @SafeVarargs
    public VNS(int[] ks, SolutionBuilder<S, I> builder, List<Shake<S, I>> shakes, Constructive<S, I> constructive, Improver<S, I>... improvers) {
        super(builder);
        if (ks == null || ks.length == 0) {
            throw new IllegalArgumentException("Invalid Ks array, must have at least one element");
        }
        this.ks = ks;
        // Ensure Ks are sorted, maxK is the last element
        Arrays.sort(ks);
        this.maxK = ks[ks.length - 1];
        this.shakes = shakes;
        this.constructive = constructive;
        this.improvers = Arrays.asList(improvers);
    }

    protected Solution<I> algorithm(I ins) {
        S solution;
        S best = null;
        do {
            solution = iteration(ins);
            if(best == null){
                best = solution;
            }
            best = best.getBetterSolution(solution);
            //System.out.println(best.getOptimalValue());
        } while (!best.stop()); // TODO Possible bug, each iteration resets the solution time counter
        return best;
    }

    private S iteration(I ins) {
        S solution = constructive.construct(ins, builder);
        solution = localSearch(solution);

        int currentKIndex = 0;
        while (currentKIndex < ks.length && !solution.stop()) {
            printStatus(String.valueOf(currentKIndex), solution);
            S bestSolution = solution;
            for(var shake: shakes){
                S copy = bestSolution.cloneSolution();
                shake.shake(copy, this.ks[currentKIndex], maxK);
                copy = localSearch(copy);
                //System.out.print(copy.getOptimalValue()+",");
                bestSolution = bestSolution.getBetterSolution(copy);
            }
            if (bestSolution == solution) {
                currentKIndex++;
            } else {
                solution = bestSolution;
                currentKIndex = 0;
            }
        }
        return solution;
    }

    private S localSearch(S solution) {
        for (Improver<S, I> ls : improvers) {
            solution = ls.improve(solution);
        }
        return solution;
    }

    private void printStatus(String phase, S s) {
        log.fine(String.format("\t\t\t%s: %s\n", phase, s));
    }

    @Override
    public String toString() {
        return "VNS{" +
                "improvers=" + improvers +
                ", constructive=" + constructive +
                ", shakes=" + shakes +
                ", ks=" + Arrays.toString(ks) +
                '}';
    }
}
