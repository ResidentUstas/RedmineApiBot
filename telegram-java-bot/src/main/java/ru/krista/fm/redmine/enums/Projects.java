package ru.krista.fm.redmine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Проекты
 */
@Getter
@AllArgsConstructor
public enum Projects {
    RIA(17, "RIA"),

    Database(7, "СУБД"),

    Workplace(2, "WorkPlace"),

    Reports(63, "Отчёты"),

    FM_WEB(187, "FM-Web"),

    AutoUpdateSystem(39, "Система автообновления"),

    WEB_fees(70, "Web-сборы");

    int redmineProjectId;
    String projectName;
}
