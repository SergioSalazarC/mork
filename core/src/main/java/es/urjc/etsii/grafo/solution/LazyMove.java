package es.urjc.etsii.grafo.solution;

import es.urjc.etsii.grafo.io.Instance;
import es.urjc.etsii.grafo.solver.Config;
import es.urjc.etsii.grafo.util.DoubleComparator;
import es.urjc.etsii.grafo.util.ValidationUtil;

import java.util.logging.Logger;

import static es.urjc.etsii.grafo.solution.Solution.MAX_DEBUG_MOVES;

/**
 * All neighborhood moves should be represented by implementations of either EagerMove or LazyMove.
 * As both are in the same package as the Solution, they may directly manipulate it.
 * See the following references for differences between an EagerMove and a LazyMove
 * @see es.urjc.etsii.grafo.solution.neighborhood.LazyNeighborhood
 * @see es.urjc.etsii.grafo.solution.neighborhood.EagerNeighborhood
 */
public abstract class LazyMove<S extends Solution<I>, I extends Instance> extends Move<S,I>{

    private static final Logger logger = Logger.getLogger(LazyMove.class.getName());

    public LazyMove(S s) {
        super(s);
    }

    /**
     * Get next move in this sequence.
     * @return the next move in this generator sequence if there is a next move, null otherwise
     */
    public abstract LazyMove<S, I> next();

}