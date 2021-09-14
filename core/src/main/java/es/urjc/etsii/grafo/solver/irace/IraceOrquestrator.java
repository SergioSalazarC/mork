package es.urjc.etsii.grafo.solver.irace;

import es.urjc.etsii.grafo.io.Instance;
import es.urjc.etsii.grafo.restcontroller.dto.ExecuteRequest;
import es.urjc.etsii.grafo.solution.Solution;
import es.urjc.etsii.grafo.solver.algorithms.Algorithm;
import es.urjc.etsii.grafo.solver.create.builder.ReflectiveSolutionBuilder;
import es.urjc.etsii.grafo.solver.create.builder.SolutionBuilder;
import es.urjc.etsii.grafo.solver.services.AbstractOrquestrator;
import es.urjc.etsii.grafo.solver.services.ExceptionHandler;
import es.urjc.etsii.grafo.solver.services.IOManager;
import es.urjc.etsii.grafo.solver.services.events.EventPublisher;
import es.urjc.etsii.grafo.solver.services.events.types.*;
import es.urjc.etsii.grafo.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

@Service
@ConditionalOnExpression(value = "'${irace.enabled}'")
public class IraceOrquestrator<S extends Solution<I>, I extends Instance> extends AbstractOrquestrator {

    private static final Logger log = Logger.getLogger(IraceOrquestrator.class.toString());
    private static final String IRACE_EXPNAME = "irace autoconfig";
    private static final String INTEGRATION_KEY = "ad7asdasdasd";

    private final boolean isMaximizing;
    private final IraceIntegration iraceIntegration;
    private final SolutionBuilder<S, I> solutionBuilder;
    private final IraceAlgorithmGenerator<S,I> algorithmGenerator;
    private final IOManager<S,I> io;
    private final Environment env;

    public IraceOrquestrator(
            @Value("${solver.maximizing}") boolean isMaximizing,
            IraceIntegration iraceIntegration,
            IOManager<S, I> io,
            List<ExceptionHandler<S, I>> exceptionHandlers,
            List<SolutionBuilder<S, I>> solutionBuilders,
            Optional<IraceAlgorithmGenerator<S, I>> algorithmGenerator, Environment env) {
        this.isMaximizing = isMaximizing;
        this.iraceIntegration = iraceIntegration;
        this.solutionBuilder = decideImplementation(solutionBuilders, ReflectiveSolutionBuilder.class);
        this.io = io;
        this.env = env;
        log.info("Using SolutionBuilder implementation: " + this.solutionBuilder.getClass().getSimpleName());

        this.algorithmGenerator = algorithmGenerator.orElseThrow(() -> new RuntimeException("IRACE mode enabled but no implementation of IraceAlgorithmGenerator has been found. Check the Mork docs section about IRACE."));
        log.info("IRACE mode enabled, using generator: " + this.algorithmGenerator.getClass().getSimpleName());

    }

    private boolean isJAR(){
        String className = this.getClass().getName().replace('.', '/');
        String protocol = this.getClass().getResource("/" + className + ".class").getProtocol();
        return protocol.equals("jar");
    }

    @Override
    public void run(String... args) {
        log.info("App started in IRACE mode, ready to start solving!");
        long startTime = System.nanoTime();
        var experimentName = List.of(IRACE_EXPNAME);
        EventPublisher.publishEvent(new ExecutionStartedEvent(experimentName));
        try{
            launchIrace();
        } finally {
            long totalExecutionTime = System.nanoTime() - startTime;
            EventPublisher.publishEvent(new ExecutionEndedEvent(totalExecutionTime));
            log.info(String.format("Total execution time: %s (s)", totalExecutionTime / 1_000_000_000));
        }
    }

    private void launchIrace() {
        log.info("Running experiment: IRACE autoconfig" );
        EventPublisher.publishEvent(new ExperimentStartedEvent(IRACE_EXPNAME, new ArrayList<>()));
        extractIraceFiles();
        long start = System.nanoTime();
        iraceIntegration.runIrace();
        long end = System.nanoTime();
        log.info("Finished running experiment: IRACE autoconfig");
        EventPublisher.publishEvent(new ExperimentEndedEvent(IRACE_EXPNAME, end - start));
    }

    private void extractIraceFiles() {
        try {
            copyWithSubstitutions(getInputPathFor("scenario.txt"), Path.of("scenario.txt"));
            copyWithSubstitutions(getInputPathFor("parameters.txt"), Path.of("parameters.txt"));
            copyWithSubstitutions(getInputPathFor("middleware.sh"), Path.of("middleware.sh"));
        } catch (IOException e){
            throw new RuntimeException("Failed extracting irace config files", e);
        }
    }

    private final Map<String, String> substitutions = Map.of(
            "__INTEGRATION_KEY__", INTEGRATION_KEY
    );

    private void copyWithSubstitutions(Path origin, Path target) throws IOException {
        String content = Files.readString(origin);
        for(var e: substitutions.entrySet()){
            content = content.replace(e.getKey(), e.getValue());
        }
        Files.writeString(target, content);
    }


    private Path getInputPathFor(String s) throws IOException {
        if(isJAR()){
            return ResourceUtils.getFile("classpath:irace/" + s).toPath();
        } else {
            return Path.of("../../src/main/resources/irace/", s);
        }
    }

    public double iraceCallback(ExecuteRequest request){
        var config = buildConfig(request);
        var instancePath = Path.of(config.getInstanceName());
        var instance = io.loadInstance(instancePath);
        var algorithm = this.algorithmGenerator.buildAlgorithm(config);
        double score = singleExecution(algorithm, instance);
        return score;
    }

    private IraceConfiguration buildConfig(ExecuteRequest request){
        if(!request.getKey().equals(INTEGRATION_KEY)){
            throw new IllegalArgumentException("Invalid integration key");
        }
        String seed = env.getProperty("seed");
        String decoded = StringUtil.b64decode(request.getConfig());
        String[] args = decoded.split("\\s+");
        Map<String, String> config = new HashMap<>();
        for(String arg: args){
            String[] keyValue = arg.split("=");
            if(config.containsKey(keyValue[0])){
                throw new IllegalArgumentException("Duplicated key: " + keyValue[0]);
            }
            config.put(keyValue[0], keyValue[1]);
        }
        String instanceName = Objects.requireNonNull(config.get("instance"));
        return new IraceConfiguration(config, instanceName, seed);
    }


    private double singleExecution(Algorithm<S,I> algorithm, I instance) {
        var solution = solutionBuilder.initializeSolution(instance);
        long startTime = System.nanoTime();
        var result = algorithm.algorithm(solution);
        long endTime = System.nanoTime();
        double score = result.getScore();
        if(isMaximizing){
            score *= -1; // Irace only minimizes
        }
        log.fine(String.format("IRACE Iteration: %s %.2g%n", score, (endTime - startTime) / 1e9));
        return score;
    }
}
