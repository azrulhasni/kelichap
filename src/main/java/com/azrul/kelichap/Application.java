package com.azrul.kelichap;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication(scanBasePackages = "com.azrul.kelichap.*")
@EnableJpaRepositories(basePackages = {"com.azrul.kelichap.repository"},
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@Theme(value = "kelichap")
@EnableJpaAuditing
@EnableEnversRepositories
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class Application implements AppShellConfigurator {

    public Application() {
    }

    public static void main(String[] args) {
//        System.setProperty("javax.net.ssl.trustStore","/truststore/cacerts");
//        System.setProperty("javax.net.ssl.trustStorePassword","changeit");
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.setViewport("width=device-width, initial-scale=1");
        settings.setPageTitle("Kelichap EDMS");
        settings.addMetaTag("author", "Azrul Hasni MADISA");
        settings.addFavIcon("icon", "icons/kelichap-favicon.png", "192x192");
        settings.addLink("shortcut icon", "icons/kelichap-favicon.ico");

    }


}
