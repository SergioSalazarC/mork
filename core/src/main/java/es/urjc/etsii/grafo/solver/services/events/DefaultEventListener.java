package es.urjc.etsii.grafo.solver.services.events;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Default Event listener responsible for sending framework events
 * via websockets and storing a copy in an EventStorage
 */
@Component
public class DefaultEventListener {
    private static Logger log = Logger.getLogger(DefaultEventListener.class.getName());

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final String eventPath = "/topic/events";

    private final MemoryEventStorage memoryEventStorage;

    protected DefaultEventListener(SimpMessagingTemplate simpMessagingTemplate, MemoryEventStorage memoryEventStorage) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.memoryEventStorage = memoryEventStorage;
    }

    @EventListener
    @Async
    public void sendToWebsocket(MorkEvent applicationEvent){
        log.fine(String.format("Sending event to websocket path %s: %s", eventPath, applicationEvent));
        memoryEventStorage.storeEvent(applicationEvent);
        simpMessagingTemplate.convertAndSend(eventPath, applicationEvent);
    }
}