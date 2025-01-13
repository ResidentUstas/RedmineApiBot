package ru.krista.fm.redmine.interfaces;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.ByteArrayResource;

public interface Report {

    /**
     * Формирование отчета в файл
     */
    ReportResult generateReport(ParameterRec[] parameters) throws Exception;

    /**
     * Сформированный отчёт
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    class ReportResult {
        /**
         * Содержимое файла
         */
        public ByteArrayResource fileBody;

        /**
         * Имя файла в файловой системе
         */
        public String fileName;

        /**
         * MIME-тип файла, для автоматического открытия соответствующей зарегистрированной программой
         */
        public String mimeType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class ParameterRec {
        /**
         * Значение параметра
         */
        public Object value;

        /**
         * Внутреннее имя параметра
         */
        public String name;

        /**
         * Внешнее имя параметра
         */
        public String text;

        public ParameterRec(Object value, String name) {
            this.value = value;
            this.name = name;
            this.text = name;
        }
    }

    @Getter
    @Setter
    class ListNodeModel {
        public Long id;
        public String value;
    }

    @Getter
    @Setter
    class TreeNodeModel {
        public Long id;
        public String value;
        public Long parentId;
    }
}
