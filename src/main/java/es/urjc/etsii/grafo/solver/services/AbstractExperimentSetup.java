package es.urjc.etsii.grafo.solver.services;

import es.urjc.etsii.grafo.io.Instance;
import es.urjc.etsii.grafo.solution.Solution;
import es.urjc.etsii.grafo.solver.algorithms.Algorithm;

import java.util.List;

@InheritedComponent
public abstract class AbstractExperimentSetup<S extends Solution<I>, I extends Instance> {
    public abstract List<Algorithm<S,I>> getAlgorithms();
}
