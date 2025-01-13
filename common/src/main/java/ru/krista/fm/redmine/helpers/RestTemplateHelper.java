package ru.krista.fm.redmine.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.krista.fm.redmine.dtos.MessageDto;

@AllArgsConstructor
public class RestTemplateHelper {
    private RestTemplate template;
    private String url;

    public void sendMessage(Double percent, String msg) throws JsonProcessingException {
        var messageDto = new MessageDto(msg, percent);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));
        ObjectMapper objectMapper = new ObjectMapper();
        var json = objectMapper.writeValueAsString(messageDto);
        var request = new HttpEntity<>(json, headers);
        template.postForEntity(url, request, String.class);
    }
}
