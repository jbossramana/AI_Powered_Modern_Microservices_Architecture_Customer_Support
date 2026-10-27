package com.example.support.rules;

import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;
import org.kie.internal.io.ResourceFactory;
import org.kie.api.builder.Results;
import org.kie.api.builder.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    @Bean
    public KieBase supportKieBase() {
        KieHelper helper = new KieHelper();
        helper.addResource(
                ResourceFactory.newClassPathResource("rules/ticket-rules.drl"),
                ResourceType.DRL);

        Results results = helper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(
                    "Drools rule compilation failed: " +
                    results.getMessages(Message.Level.ERROR));
        }

        return helper.build();
    }
}
