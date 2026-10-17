package pl.pcz.io2.fishquizfrontend.service;

public class ActionLogger {
    public void logAction(String action, String details) {
        System.out.printf("[LOG] %s - %s%n", action, details);
    }
}