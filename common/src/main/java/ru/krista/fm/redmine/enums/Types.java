package ru.krista.fm.redmine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.krista.fm.redmine.exceptions.ExportServiceException;

public class Types {

    /**
     * Точность вывода фактов.<br>
     * Привязываем к определенному диапазону для упрощения интерпретации значения параметра в отчетах.
      */
    @Getter
    @AllArgsConstructor
    public enum Precisions {
        N0(0, "целое значение"),
        N1(1, "с десятыми после запятой"),
        N2(2, "с сотыми после запятой"),
        N3(3, "с тысячными после запятой");

        private final Integer precision;
        private final String description;
    }

    @Getter
    @AllArgsConstructor
    public enum Multipliers {
        X1(1, 0, "руб."),
        X3(1000, 1,"тыс. руб."),
        X6(1000000, 2,"млн. руб."),
        X9(1000000000, 3,"млрд. руб.");

        private final Integer multiplier;
        private final Integer number;
        private final String description;

        @Override
        public String toString() {
            return description;
        }

        public static Multipliers getItem(Integer code) throws ExportServiceException {
            for (var item : Multipliers.values()) {
                if (item.multiplier.equals(code)) return item;
            }
            throw new ExportServiceException("Элемент не найден в перечислении «Multipliers».");
        }

        public static Multipliers getItemByNumber(Integer number) throws ExportServiceException {
            for (var item : Multipliers.values()) {
                if (item.number.equals(number)) return item;
            }
            throw new ExportServiceException("Элемент не найден в перечислении «Multipliers».");
        }
    }

    @AllArgsConstructor
    @Getter
    public enum BudgetLevels {
        /**
         * Все уровни
         */
        All(0L),

        /**
        * Фед.бюджет
        */
        Federal(1L),

        /**
        * Конс.бюджет субъекта
        */
        SubjectCons(2L),

        /**
        * Бюджет субъекта
        */
        Subject(3L),

        /**
        * Конс.бюджет МР
        */
        MRCons(4L),

        /**
        * Бюджет района
        */
        MR(5L),

        /**
        * Бюджет поселения
        */
        Settlement(6L),

        /**
        * Внебюдж.фонды
        */
        Fund(7L),

        /**
        * Пенсионный фонд
        */
        PensFund(8L),

        /**
        * Фонд соц.страхования
        */
        SocFund(9L),

        /**
        * Фед.фонд ОМС
        */
        MedFundFederal(10L),

        /**
        * Территор.фонды ОМС
        */
        MedFundTerritorial(11L),

        /**
        * УФК Смоленск
        */
        Smolensk(12L),

        /**
        * Бюджет Тюменской обл.
        */
        Tumen(13L),

        /**
        * Конс.бюджет МО
        */
        MunicipalCons(14L),

        /**
        * Бюджет ГО
        */
        City(15L),

        /**
        * Бюджет ГП
        */
        Town(16L),

        /**
        * Бюджет СП
        */
        Village(17L),

        /**
        * Бюджет внутригородских МО
        */
        Township(18L),

        /**
        * Конс. бюджет ГО с ВГД
        */
        CityWithDistrictsCons(19L),

        /**
        * Бюджет ГО с ВГД
        */
        CityWithDistricts(20L),

        /**
        * Бюджет ВГР
        */
        District(21L),

        /**
        * Бюджет муниципального округа
        */
        MO(23L);

        private final Long id;
    }

    /**
     * Типы территорий
     */
    @Getter
    @AllArgsConstructor
    public enum TerrTypes {
        /**
         * Российская Федерация
         */
        RF(1),

        /**
         * Федеральный округ
         */
        FO(2),

        /**
         * Субъект РФ
         */
        SB(3),

        /**
         * Муниципальный район
         */
        MR(4),

        /**
         * Городское поселение
         */
        GP(5),

        /**
         * Сельское поселение
         */
        SP(6),

        /**
         * Городской округ
         */
        GO(7),

        /**
         * Внутригородская территория города федерального значения
         */
        VTGFZ(8),

        /**
         * Межселенные территории
         */
        MegsTer(9),

        /**
         * Районный центр
         */
        RC(10),

        /**
         * Поселение
         */
        POS(11),

        /**
         * Населенный пункт
         */
        NP(12),

        /**
         * Городской округ с внутригородским делением
         */
        GOVGD(13),

        /**
         * Внутригородской район
         */
        VGR(14),

        /**
         * Муниципальный округ
         */
        MO(15);

        private Integer id;
    }

    /**
     * Периоды из 4 отчёта Бурятии.
     */
    @AllArgsConstructor
    @Getter
    public enum PeriodTypes {
        prevYearPeriod(1),

        prevYear(2),

        currentPeriod(3);

        int periodId;
    }

    /**
     * Вид параметров источника
     */
    @RequiredArgsConstructor
    public enum DataSourceTypes {
        NoDivide(-1, "Не делится"),
        Budget(0, "Бюджет"),
        Year(1, "Год"),
        YearMonth(2, "Год месяц"),
        YearMonthVariant(3, "Год месяц вариант"),
        YearVariant(4, "Год вариант"),
        YearQuarter(5, "Год квартал"),
        YearTerritory(6, "Год территория"),
        YearQuarterMonth(7, "Год квартал месяц"),
        WithoutParams(8, "Без параметров"),
        Variant(9, "Вариант"),
        YearMonthTerritory(10, "Год месяц территория"),
        YearQuarterTerritory(11, "Год квартал территория"),
        YearVariantMonthTerritory(12, "Год вариант месяц территория"),
        Classifiers(22, "Классификаторы");

        private final Integer Code;
        private final String name;

        public Integer toInt() {
            return this.Code;
        }

        public static DataSourceTypes itemById(Integer id) throws ExportServiceException {
            for (var item : DataSourceTypes.values()) {
                if (item.Code.equals(id)) return item;
            }
            throw new ExportServiceException("Элемент не найден в перечислении «DataSourceTypes».");
        }
    }

    /**
     * Зарплата.Статистические формы - d.Salary.StatForma
     */
    @Getter
    @AllArgsConstructor
    public enum StatForma {
        /**
         * Значение не указано
         */
        Undefined(0),

        /**
         * № ЗП-здрав
         */
        HealthCare(1),

        /**
         * № ЗП-наука
         */
        Science(2),

        /**
         * № ЗП-образование
         */
        Education(3),

        /**
         * № ЗП-социалка
         */
        Social(4),

        /**
         * № ЗП-культура
         */
        Culture(5),

        /**
         * № ЗП-прочие учреждения
         */
        Other(6);

        int reportFormId;
    }

    @Getter
    @AllArgsConstructor
    public enum MarkInstCodes
    {
        /// <summary>
        /// Средняя численность работников, человек
        /// списочного состава (без внешних совместителей)
        /// </summary>
        MarkAveAll(10100),

        /// <summary>
        /// Средняя численность работников, человек
        /// внешних совместителей
        /// </summary>
        MarkAveExtOffHourWork(10200),

        /// <summary>
        /// Фонд начисленной заработной платы работников за отчетный период, тыс. руб
        /// списочного состава (без внешних совместителей)
        /// всего
        /// </summary>
        MarkFondAll(20101),

        /// <summary>
        /// Фонд начисленной заработной платы работников за отчетный период, тыс. руб
        /// списочного состава (без внешних совместителей)
        /// в том числе по внутреннему совместительству
        /// </summary>
        MarkFondIntOffHourWork(20102),

        /// <summary>
        /// Фонд начисленной заработной платы работников за отчетный период, тыс. руб
        /// внешних совместителей
        /// </summary>
        MarkFondExtOffHourWork(20200),

        /// <summary>
        /// Фонд начисленной заработной платы работников по источникам финансирования, тыс. руб
        /// из гр. 3 списочного состава (без внешних совместителей)
        /// (за счет бюджетов всех уровней (субсидий))
        /// </summary>
        MarkFondSourcesGr3Subsidy(30101),

        /// <summary>
        /// Фонд начисленной заработной платы работников по источникам финансирования, тыс. руб
        /// из гр. 3 списочного состава (без внешних совместителей)
        /// (ОМС)
        /// </summary>
        MarkFondSourcesGr3Oms(30102),

        /// <summary>
        /// Фонд начисленной заработной платы работников по источникам финансирования, тыс. руб
        /// из гр. 3 списочного состава (без внешних совместителей)
        /// (средства от приносящей доход деятельности)
        /// </summary>
        MarkFondSourcesGr3Activity(30103),

        /// <summary>
        /// Фонд начисленной заработной платы работников по источникам финансирования, тыс. руб
        /// из гр. 5 внешних совместителей
        /// (за счет бюджетов всех уровней (субсидий))
        /// </summary>
        MarkFondSourcesGr5Subsidy(30201),

        /// <summary>
        /// Фонд начисленной заработной платы работников по источникам финансирования, тыс. руб
        /// из гр. 5 внешних совместителей
        /// (ОМС)
        /// </summary>
        MarkFondSourcesGr5Oms(30202),

        /// <summary>
        /// Фонд начисленной заработной платы работников по источникам финансирования, тыс. руб
        /// из гр. 5 внешних совместителей
        /// (средства от приносящей доход деятельности)
        /// </summary>
        MarkFondSourcesGr5Activity(30203),

        /// <summary>
        /// Фонд начисленной заработной платы работников по источникам финансирования, тыс. руб
        /// из гр. 6 за счет средств федерального бюджета, начисленных за выполнение педагогическими работниками функций классного руководителя (куратора)
        /// </summary>
        Mark030300(30300),

        /// <summary>
        /// Средняя численность работников по источникам финансирования
        /// из гр. 1 списочного состава (без внешних совместителей)
        /// счет средств бюджетов всех уровней (субсидий)
        /// </summary>
        Mark040101(40101),

        /// <summary>
        /// Средняя численность работников по источникам финансирования
        /// из гр. 1 списочного состава (без внешних совместителей)
        /// ОМС
        /// </summary>
        Mark040102(40102),

        /// <summary>
        /// Средняя численность работников по источникам финансирования
        /// из гр. 1 списочного состава (без внешних совместителей)
        /// средства от приносящей доход деятельности
        /// </summary>
        Mark040103(40103),

        /// <summary>
        /// Средняя численность работников по источникам финансирования
        /// из гр. 2 внешних совместителей
        /// счет средств бюджетов всех уровней (субсидий)
        /// </summary>
        Mark050101(50101),

        /// <summary>
        /// Средняя численность работников по источникам финансирования
        /// из гр. 2 внешних совместителей
        /// ОМС
        /// </summary>
        Mark050102(50102),

        /// <summary>
        /// Средняя численность работников по источникам финансирования
        /// из гр. 2 внешних совместителей
        /// средства от приносящей доход деятельности
        /// </summary>
        Mark050103(50103),

        /// <summary>
        /// Штатная численность, единиц
        /// </summary>
        MarkStaffList(60000);

        private final int code;
    }
}
