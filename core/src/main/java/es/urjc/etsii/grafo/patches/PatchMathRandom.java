package es.urjc.etsii.grafo.patches;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.logging.Logger;

@Service
public class PatchMathRandom {

    private static final Logger log = Logger.getLogger(PatchMathRandom.class.getName());

    private final boolean isEnabled;

    public PatchMathRandom(@Value("${advanced.block.math-random}") boolean isEnabled){
        this.isEnabled = isEnabled;
    }

    @PostConstruct
    public void patch(){
        if(!isEnabled){
            log.info("Skipping Math.random() patch");
            return;
        }

        try {
            var internalClass = Class.forName("java.lang.Math$RandomNumberGeneratorHolder");
            var internalRandom = internalClass.getDeclaredField("randomNumberGenerator");
            internalRandom.setAccessible(true);
            makeNonFinal(internalRandom);
            internalRandom.set(null, new FailRandom());
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            log.warning("Failed to patch Math.random()");
            throw new RuntimeException(e);
        }
        log.info("Math.random() patched successfully");
    }

    private static final VarHandle MODIFIERS;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void makeNonFinal(Field field) {
        int mods = field.getModifiers();
        if (Modifier.isFinal(mods)) {
            MODIFIERS.set(field, mods & ~Modifier.FINAL);
        }
    }
}