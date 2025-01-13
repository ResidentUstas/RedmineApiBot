package ru.krista.fm.redmine.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class MessageDto {
    private String message;
    private double percent;
}