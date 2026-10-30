package pl.pcz.io2.fishquizfrontend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Akcje użytkownika (dodanie, edycja, usunięcie...) trafiają do wspólnego logu slf4j
public class ActionLogger {
    private static final Logger log = LoggerFactory.getLogger(ActionLogger.class);

    public void logAction(String action, String details) {
        log.info("{} - {}", action, details);
    }
}
