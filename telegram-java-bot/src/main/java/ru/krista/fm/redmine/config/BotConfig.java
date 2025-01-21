package ru.krista.fm.redmine.config;

import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.internal.Transport;
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

    @Bean
    public RedmineManager getRedmineManager(){
        String uri = "http://fmredmine.krista.ru/";
        String apiAccessKey = "a84e0c96ec3e7b9142f1c06dc8e176db72dd31ea";

        RedmineManager mgr = RedmineManagerFactory.createWithApiKey(uri, apiAccessKey);
        mgr.setObjectsPerPage(100);
        Transport transport = mgr.getTransport();
        return mgr;
    }
}