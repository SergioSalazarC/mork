package es.urjc.etsii.grafo.io;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import es.urjc.etsii.grafo.solution.Solution;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;

public class DefaultJSONSolutionSerializer<S extends Solution<I>, I extends Instance> extends SolutionSerializer<S,I>{

    ObjectWriter writer;

    @Value("${serializers.sol-json.enabled}")
    private boolean enabled;

    @Value("${serializers.sol-json.pretty}")
    private boolean pretty;

    public DefaultJSONSolutionSerializer() {
        var mapper = new ObjectMapper();
        if(pretty){
            writer = mapper.writer(new DefaultPrettyPrinter());
        } else {
            writer = mapper.writer();
        }
    }

    @Override
    public void export(File f, S s) {
        if(enabled){
            try {
                writer.writeValue(f,s);
            } catch (IOException e){
                throw new RuntimeException("IOException while writing to file: "+f.getAbsolutePath(), e);
            }
        }
    }
}