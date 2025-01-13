package ru.krista.fm.redmine.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Getter
@Configuration
@Data
public class BotConfig {
    @Value("${bot.name}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    @Bean
    public DefaultBotOptions getDefaultBotOptions(){
        DefaultBotOptions botOptions = new DefaultBotOptions();
        botOptions.setProxyHost("proxy.krista.ru");
        botOptions.setProxyPort(8080);
        botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
        return botOptions;
    }

}