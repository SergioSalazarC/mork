package es.urjc.etsii.grafo.autoconfig.controller;

import es.urjc.etsii.grafo.autoconfig.irace.AutomaticIraceAlgorithmGenerator;
import es.urjc.etsii.grafo.autoconfig.irace.IraceOrchestrator;
import es.urjc.etsii.grafo.autoconfig.service.AlgorithmCandidateGenerator;
import es.urjc.etsii.grafo.autoconfig.service.AlgorithmInventoryService;
import es.urjc.etsii.grafo.config.SolverConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@ConditionalOnExpression(value = "${solver.autoconfig}")
public class AutoconfigDebugController {

    private final SolverConfig solverConfig;
    private final AlgorithmInventoryService inventory;
    private final AlgorithmCandidateGenerator candidateGenerator;
    private final AutomaticIraceAlgorithmGenerator<?, ?> algorithmGenerator;
    private final IraceOrchestrator<?, ?> orchestrator;

    public AutoconfigDebugController(SolverConfig solverConfig, AlgorithmInventoryService inventory, AlgorithmCandidateGenerator candidateGenerator, AutomaticIraceAlgorithmGenerator<?, ?> algorithmGenerator, IraceOrchestrator<?, ?> orchestrator) {
        this.solverConfig = solverConfig;
        this.inventory = inventory;
        this.candidateGenerator = candidateGenerator;
        this.algorithmGenerator = algorithmGenerator;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/auto/debug/tree")
    public Object getTree(){
        return this.candidateGenerator.buildTree(solverConfig.getTreeDepth());
    }

    @GetMapping("/auto/debug/params")
    public Object params(){
        return this.candidateGenerator.componentParams();
    }

    @GetMapping("/auto/debug/inventory")
    public Object getInventory(){
        return this.inventory.getInventory();
    }

    @GetMapping("/auto/debug/decode/**")
    public Object decode(HttpServletRequest request){
        String url = request.getRequestURI();
        String cmdline = url.split("/decode/")[1];
        cmdline = URLDecoder.decode(cmdline, StandardCharsets.UTF_8);
        var config = IraceOrchestrator.toIraceRuntimeConfig(cmdline);
        var algorithmString = this.algorithmGenerator.buildAlgorithmString(config.getAlgorithmConfig());
        return Map.of("config", config, "algorithmString", algorithmString);
    }

    @GetMapping("/auto/debug/history")
    public Object historic(){
        return this.orchestrator.getConfigHistoric();
    }


}
