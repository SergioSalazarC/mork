package es.urjc.etsii.grafo.solver.services.events;

import es.urjc.etsii.grafo.solver.services.events.types.ErrorEvent;
import es.urjc.etsii.grafo.solver.services.events.types.ExecutionEndedEvent;
import es.urjc.etsii.grafo.solver.services.events.types.ExperimentEndedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.logging.Logger;

public class TelegramEventListener extends AbstractEventListener {

    private static final Logger log = Logger.getLogger(MorkTelegramBot.class.getName());
    private static final int longPollingTimeoutInSeconds = 10;

    private final boolean enabled;
    private volatile boolean errorNotified = false;

    private MorkTelegramBot telegramBot;
    private BotSession session;

    public TelegramEventListener(
            @Value("${event.telegram.enabled:false}") boolean enabled,
            @Value("${event.telegram.chatId:none}") String chatId,
            @Value("${event.telegram.token}") String token
    ) {
        this.enabled = enabled;
        if (enabled) {
            log.info("Registering Telegram bot...");
            var options = new DefaultBotOptions();
            options.setGetUpdatesTimeout(longPollingTimeoutInSeconds);
            this.telegramBot = new MorkTelegramBot(chatId, token, options);
            try {
                var api = new TelegramBotsApi(DefaultBotSession.class);
                session = api.registerBot(telegramBot);
                log.info("Telegram integration enabled");
            } catch (TelegramApiException e) {
                log.warning("Failed bot registration: " + e);
                telegramBot = null;
            }
        }
    }

    @MorkEventListener
    public void onExperimentEnd(ExperimentEndedEvent event){
        if(telegramBot != null && telegramBot.ready()){
            telegramBot.sendMessage(String.format("Experiment %s ended. Execution time: %s seconds", event.getExperimentName(), event.getExecutionTime() / 1_000_000_000));
        }
    }

    @MorkEventListener
    public void onError(ErrorEvent event){
        if(telegramBot != null && telegramBot.ready()){
            // Only notify first error to prevent spamming
            if(!errorNotified){
                errorNotified = true;
                var t = event.getThrowable();
                telegramBot.sendMessage(String.format("Execution Error: %s. Further errors will NOT be notified.", t));
            }

        }
    }

    @MorkEventListener
    public void onExecutionEnd(ExecutionEndedEvent event){
        if(!enabled) return;

        log.info("Stopping telegram bot... This can take up to 10 seconds.");
        if(session != null){
            session.stop();
        }
        log.info("Stopped telegram bot.");
    }

    private static class MorkTelegramBot extends TelegramLongPollingBot {

        private static final Logger log = Logger.getLogger(MorkTelegramBot.class.getName());

        private final String chatId;
        private final String token;

        private MorkTelegramBot(String chatId, String token, DefaultBotOptions options) {
            super(options);
            this.chatId = chatId;
            this.token = token;
        }

        @Override
        public String getBotUsername() {
            return "MorkTelegramIntegration";
        }

        @Override
        public String getBotToken() {
            return token;
        }

        @Override
        public void onUpdateReceived(Update update) {
            log.info(String.format("Recieved message %s", update));
            if(update.hasMessage()){
                var message = update.getMessage();
                var chatId = message.getChatId().toString();
                var action = new SendMessage(chatId, String.format("Chat id: %s", chatId));
                try {
                    this.execute(action);
                } catch (TelegramApiException e) {
                    log.info("Failed sending chatId to user: " + e);
                }
            }
        }

        public void sendMessage(String str){
            var action = new SendMessage(this.chatId, str);
            try {
                this.execute(action);
            } catch (TelegramApiException e) {
                log.warning("Failed to send message:" + e);
            }
        }

        public boolean ready(){
            return !this.chatId.equals("none");
        }
    }
}