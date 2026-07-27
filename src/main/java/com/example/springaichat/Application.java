package com.example.springaichat;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Two annotations carry real weight here:
 *
 * <ul>
 *   <li>{@code @Push} — server push is what lets the server send tokens to the browser
 *       as the model generates them. Without it the UI would only update on the next
 *       client-initiated round trip, so the whole answer would appear at once.
 *   <li>{@code @StyleSheet} — Vaadin 25 applies themes this way ({@code @Theme} is
 *       deprecated). Lumo must be imported before our own CSS so that custom rules win
 *       on specificity.
 * </ul>
 */
@SpringBootApplication
@EnableConfigurationProperties(AssistantProperties.class)
@Push
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
