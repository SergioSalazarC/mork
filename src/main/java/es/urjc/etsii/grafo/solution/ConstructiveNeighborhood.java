package es.urjc.etsii.grafo.solution;

import es.urjc.etsii.grafo.io.Instance;

/**
 *
 */
public abstract class ConstructiveNeighborhood
        <
            S extends Solution<I>,
            I extends Instance
        >
        extends Neighborhood<S, I> {

}