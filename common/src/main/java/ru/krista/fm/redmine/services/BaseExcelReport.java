package ru.krista.fm.redmine.services;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import ru.krista.fm.redmine.annotations.ReportCode;
import ru.krista.fm.redmine.exceptions.ExportServiceArgumentNullException;
import ru.krista.fm.redmine.exceptions.ExportServiceException;
import ru.krista.fm.redmine.helpers.ReportHelper;
import ru.krista.fm.redmine.interfaces.Report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.DataFormatException;

import static org.apache.poi.util.StringUtil.isNotBlank;

@RequiredArgsConstructor
@Service
@Slf4j
public abstract class BaseExcelReport implements Report {
    protected Workbook workbook;

    @Getter
    protected SheetParameters currentParams;

    protected String mimeType;

    protected String getMimeType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Getter
    @Setter
    protected String fileName = "report.xlsx";

    @Getter
    protected ArrayList<SheetParameters> sheetParams = new ArrayList<>();

    protected Map<String, Object> replacements = new LinkedHashMap<>();

    @Override
    public ReportResult generateReport(ParameterRec[] parameters)
            throws Exception {
        String reportCode = "Экспорт в Excel";
        var isExistReportCodeAnnotation = getClass().isAnnotationPresent(ReportCode.class);
        if (isExistReportCodeAnnotation) reportCode = getClass().getAnnotation(ReportCode.class).value();

        setWorkbook(reportCode);

        if (setup(parameters)) fillReportData();

        complete();
        return new ReportResult(getResultStream(), getFileName(), getMimeType());
    }

    protected void fillReportData() throws Exception {
    }

    protected Boolean setup(ParameterRec[] repParams) throws ExportServiceException, ParseException, ExportServiceArgumentNullException {
        return false;
    }

    /**
     * Для закрепления шапки таблиц. Создаёт область закрепления.
     * rightColumnOfArea - правая крайняя колонка области; bottomRowOfArea - нижний ряд облости
     */
    public static void setFreezeArea(Sheet sheet, int rightColumnOfArea, int bottomRowOfArea) {
        sheet.createFreezePane(rightColumnOfArea, bottomRowOfArea);
    }

    /**
     * Сохраняем "книгу" и формируем вывод файла
     */
    protected ByteArrayResource getResultStream() throws IOException {
        var outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new ByteArrayResource(outputStream.toByteArray(), fileName);
    }

    /*
     *   Берем шаблон из папки resources/templates
     */
    protected void setWorkbook(String fName) throws IOException, DataFormatException, ExportServiceException {
        if (fName == null) return;
        log.info("Шаблон загружен: {}", fName);
        var path = "templates/" + fName;
        var classLoader = getClass().getClassLoader();
        var file = classLoader.getResourceAsStream(path);
        assert file != null;
        this.workbook = new XSSFWorkbook(file);
    }

    protected void deleteSourcesRows(Sheet sheet) {
        sheet.shiftRows(getCurrentParams().getFirstDataRow(), sheet.getLastRowNum(), -getCurrentParams().getStyleRowsCount() + 1);
    }

    protected void deleteFromSheetParams(List<Integer> indexesList) {
        var reversedCopy = new ArrayList<>(indexesList);
        Collections.reverse(reversedCopy);
        for (int index : reversedCopy) {
            this.sheetParams.remove(index);
        }
    }

    protected void setCurrentSheet(int i) {
        currentParams = getSheetParams().stream().filter(x -> x.getSheetNum() == i).findFirst().orElse(null);
    }

    /*
     *   Замена кодовых слов на всех листах шаблона
     */
    protected void makeReplacement() {
        if (replacements.isEmpty()) return;
        var sheets = getSheetParams().stream().map(x -> x.sheet).toList();
        for (var sheet : sheets) {
            for (var row : sheet) {
                for (var cell : row) {
                    for (var r : replacements.entrySet()) {
                        if (cell.getCellType() == CellType.STRING) {
                            var text = cell.getStringCellValue();
                            if (text != null && text.contains(r.getKey())) {
                                text = text.replace(r.getKey(), r.getValue().toString());
                                cell.setCellValue(text);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void mergedCellsUp(int column, int count) {
        if (count <= 1) return;
        currentParams.sheet.addMergedRegion(new CellRangeAddress(
                getCurrentParams().getCurrentRow() - count + 1,
                getCurrentParams().getCurrentRow(),
                column,
                column));
    }

    protected void mergedCellsUp(int column, int startRow, int count) {
        if (count <= 1) return;
        currentParams.sheet.addMergedRegion(new CellRangeAddress(
                startRow - count + 1,
                startRow,
                column,
                column));
    }

    protected void mergedCells(int firstColumn, int lastColumn, int lastRow, int count) {
        if (count <= 1) return;
        currentParams.sheet.addMergedRegion(new CellRangeAddress(
                lastRow - count + 1,
                lastRow,
                firstColumn,
                lastColumn));
    }

    protected void addRow(int rowStyle, Object... values) {
        if (rowStyle < 0 || rowStyle >= getCurrentParams().getStyleRowsCount()) return;

        var sheet = getCurrentParams().getSheet();
        currentParams.currentRow += currentParams.currentRowHeight != null ? currentParams.currentRowHeight : 1;
        currentParams.currentRowHeight = 1;
        currentParams.currentCell = -1;
        currentParams.currentRowStyle = rowStyle;

        var styleRowIndex = getCurrentParams().getFirstDataRow() + rowStyle;
        var copiedRow = sheet.getRow(styleRowIndex);
        var rows = new ArrayList<Row>(); // Стандартному методу copyRows передается список копируемых строк
        rows.add(copiedRow);

        var startRow = getCurrentParams().getCurrentRow();
        var rowsCount = 1;
        var needToCopyRowHeight = true;
        var needToResetOriginalRowHeight = false;

        var endRow = sheet.getLastRowNum();
        if (endRow < startRow) endRow += startRow;

        sheet.shiftRows(startRow, endRow, rowsCount, needToCopyRowHeight, needToResetOriginalRowHeight);

        var sheetTarget = (XSSFSheet) sheet;
        sheetTarget.copyRows(rows, getCurrentParams().getCurrentRow(), new CellCopyPolicy()); // Копируем указанную стилевую строку
        if (values.length != 0) addCells(values); // Заполняем ячейки значениями
    }

    protected void deleteSourceRows(Sheet sheet, int startRow, int endRow, int rowsCount) {
        sheet.shiftRows(startRow, endRow, rowsCount, true, true);
    }

    protected void addCells(Object... values) {
        if (values == null || values.length == 0) {
            getPastedObject(null).paste(this);
            return;
        }

        for (var value : values) {
            getPastedObject(value).paste(this);
        }
    }

    protected void setCurrentRowThinBorderBottom() {
        var targetRow = getCurrentParams().getCurrentRow() + 1;
        RegionUtil.setBorderBottom(
                BorderStyle.THIN,
                CellRangeAddress.valueOf(
                        getCurrentParams().getFirstDataCol()
                                + targetRow
                                + ":"
                                + getCurrentParams().getLastDataCol()
                                + targetRow),
                getCurrentParams().getSheet()); // Устанавливаем нижнюю границу строки в указанном диапазоне
    }

    protected void setRowThinBorderTopFirstDataRow(SheetParameters sheetParameters, String firstCol, String lastCol) {
        if (sheetParameters != null) {
            var targetRow = sheetParameters.getFirstDataRow() + 1;
            RegionUtil.setBorderTop(
                    BorderStyle.THIN,
                    CellRangeAddress.valueOf(firstCol + targetRow + ":" + lastCol + targetRow),
                    sheetParameters.getSheet()); // Устанавливаем нижнюю границу строки в указанном диапазоне
        }
    }

    protected String getValue(Object value) {
        if (value == null) return "";
        else return (String) value;
    }

    protected PastedObject getPastedObject(Object value) {
        if (value instanceof PastedObject) return (PastedObject) value;
        return new PastedObject(value);
    }

    protected void complete() {
        for (var sheetParam : sheetParams) {
            var sheet = workbook.getSheetAt(sheetParam.sheetNum);
            // Удаляем стилевые строки
            if (sheetParam.getStyleRowsCount() > 0 && sheet.getLastRowNum() > 0) {
                // Удаляем стилевые строки путём натаскивания остальных строк на них
                int startPosition = sheetParam.firstDataRow + sheetParam.getStyleRowsCount();
                int endPositon = sheet.getLastRowNum() + 16;
                if (startPosition < endPositon) {
                    sheet.shiftRows(
                            startPosition,
                            endPositon,
                            -sheetParam.getStyleRowsCount());
                }
            }
        }

        clearSheetParams();
    }

    protected void clearSheetParams() {
        sheetParams.clear();
    }

    /**
     * Метод необходим для настройки авто-высоты строк
     */
    private void setLastModifiedCol(SheetParameters parameters) {
        var lastColName = parameters.lastDataCol;
        parameters.lastModifiedCol = CellRangeAddress.valueOf(lastColName).getLastColumn();
    }

    /**
     * "Расчетная авто-высота строк"
     */
    protected void autoSizeRow(Sheet sheet, int rowNum, int lastModifiedCol) {
        var allMergedRegions = sheet.getMergedRegions();
        var mergedRegsOnRow = getMergedRegsOnRow(sheet, rowNum, lastModifiedCol);
        var symbolWidth = 2f; // Примерная ширина символа при шрифте в 11 пт
        var cellWidth = 20f; // Примерная ширина ячейки в отчете, равная 2 см
        var tallestCell = -1f; // Самая "высокая" ячейка

        var row = sheet.getRow(rowNum);
        if (row == null) {
            return;
        }

        for (var mRegNum : mergedRegsOnRow) {
            var mReg = allMergedRegions.get(mRegNum);
            if (mReg.getFirstColumn() == mReg.getLastColumn()) {
                continue;
            }

            var numLines = 1; // "Количество строк" в объединенных ячейках
            var mRegLength = mReg.getNumberOfCells();
            var cell = (XSSFCell) row.getCell(mReg.getFirstColumn());
            var fontSize = cell.getCellStyle().getFont().getFontHeightInPoints();

            if (cell.getCellType() == CellType.STRING) {
                var firstCellValLength = cell.getStringCellValue().length();
                if (firstCellValLength != 0) {
                    var valWidth = symbolWidth * firstCellValLength;
                    var mRegWidth = cellWidth * mRegLength;
                    if (valWidth > mRegWidth) {
                        var height = Math.ceil(valWidth / mRegWidth);
                        numLines += (int) height;
                    }
                }
            }

            var cellHeight = computeRowHeightInPoints(sheet, fontSize, numLines);
            if (cellHeight > tallestCell) {
                tallestCell = cellHeight;
            }
        }

        var defaultRowHeightInPoints = sheet.getDefaultRowHeightInPoints();
        var rowHeight = tallestCell;
        if (rowHeight < defaultRowHeightInPoints + 1) {
            rowHeight = -1; // Сброс высоты строки
        }

        row.setHeightInPoints(rowHeight);
    }

    protected void autoSizeColumns() {
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getPhysicalNumberOfRows() > 0) {
                var row = sheet.getRow(sheet.getFirstRowNum());
                var cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    var cell = cellIterator.next();
                    int columnIndex = cell.getColumnIndex();
                    sheet.autoSizeColumn(columnIndex);
                    int currentColumnWidth = sheet.getColumnWidth(columnIndex);
                    sheet.setColumnWidth(columnIndex, (currentColumnWidth + 2500));
                }
            }
        }
    }

    protected boolean isEmptyRow(Row row) {
        var isEmptyRow = true;
        for (var cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            var cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK && isNotBlank(cell.toString())) isEmptyRow = false;
        }
        return isEmptyRow;
    }

    /**
     * Добавление комментария к текущей ячейке
     *
     * @param commentText Текст комментария
     * @param font        Шрифт комментария
     */
    protected void addCommentToCurrentCell(String commentText, Font font) {
        var curCell = getOrCreateCurrentCell();
        addComment(curCell, commentText, font);
    }

    /**
     * Добавление комментария к ячейке
     *
     * @param cell        Ячейка
     * @param commentText Текст Комментария
     * @param font        Шрифт комментария
     */
    protected void addComment(Cell cell, String commentText, Font font) {
        var creationHelper = workbook.getCreationHelper();
        var anchor = creationHelper.createClientAnchor();

        // Устанавливаем размер комментария, который имеет вид "квадрата"
        anchor.setCol1(cell.getColumnIndex() + 1); // Начало отображаемого "квадрата" комментария, левый край - колонка
        anchor.setCol2(cell.getColumnIndex() + 2); // Конец отображаемого "квадрата" комментария, правый край - колонка
        anchor.setRow1(cell.getRowIndex() + 1); // Начало комментария, верхний край - строка
        anchor.setRow2(cell.getRowIndex() + 3); // Конец комментария, нижний край - строка

        var drawing = cell.getSheet().createDrawingPatriarch();
        var comment = drawing.createCellComment(anchor);
        var text = creationHelper.createRichTextString(commentText);
        if (font != null) text.applyFont(font);

        comment.setRow(cell.getRowIndex());
        comment.setColumn(cell.getColumnIndex());

        comment.setString(text); // Текст комментария
//        comment.setAuthor(author); // Устанавливаем автора
        cell.setCellComment(comment);
    }

    protected void setCellStyleToCurrentCell(CellStyle style) {
        var cell = getOrCreateCurrentCell();
        cell.setCellStyle(style);
    }

    protected Cell getOrCreateCurrentCell() {
        return getOrCreateCell(currentParams.getSheet(), currentParams.getCurrentRow(), currentParams.getCurrentCell());
    }

    protected Cell getOrCreateCell(Sheet sheet, int rowIdx, int colIdx) {
        var row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);

        var cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        return cell;
    }

    protected ArrayList<Integer> getMergedRegsOnRow(Sheet sheet, int rowNum, int lastModifiedCol) {
        var mergedRegsOnRow = new ArrayList<Integer>();
        for (int col = 0; col <= lastModifiedCol; col++) {
            var mergeReg = getIndexIfCellIsInMergedCells(sheet, rowNum, col);
            if (mergeReg != null && !mergedRegsOnRow.contains(mergeReg)) {
                mergedRegsOnRow.add(mergeReg);
            }
        }
        return mergedRegsOnRow;
    }

    @RequiredArgsConstructor
    protected class SkipValue extends PastedObject {
        private final int count;

        @Override
        public void paste(BaseExcelReport report) {
            var sp = report.getCurrentParams();
            sp.setCurrentCell(sp.getCurrentCell() + count);
            var cell = workbook.getSheetAt(sp.getSheetNum()).getRow(sp.getCurrentRow()).getCell(sp.getCurrentCell());
            skipMergeRegion(cell);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    protected class PastedObject {
        private Object value;

        public void paste(BaseExcelReport report) {
            var sp = report.getCurrentParams();
            sp.setCurrentCell(sp.getCurrentCell() + 1);

            var cellValue = (sp.getSumColumns().contains(sp.getCurrentCell()) && value instanceof Double)
                    ? (Double) value / sp.getDivider()
                    : value;

            var cell = getOrCreateCell(sp.getSheet(), sp.getCurrentRow(), sp.getCurrentCell());
            setCellValue(cell, cellValue);
        }
    }

    @Getter
    @Setter
    protected class SheetParameters {
        private Integer sheetNum;
        private Integer firstDataRow;
        private Integer currentRow;
        private Integer styleRowsCount;
        private Integer currentCell;
        private Integer currentRowStyle;
        private Integer currentRowHeight;
        private Integer lastModifiedCol;
        private Integer countWBMergedRegions;
        private Sheet sheet;
        private List<Sheet> sheetQueue;
        private Boolean needTopBorderFirstDataRow;
        private String firstDataCol;
        private String lastDataCol;
        private Integer divider;
        private List<Integer> sumColumns = new ArrayList<>();
        private String sumFormat = "#,##0.00";

        public void setSumColumns(List<Integer> sumColumns) {
            this.sumColumns = sumColumns;
            setSumColumnsFormat();
        }

        public SheetParameters() {
            this.firstDataRow = 0;
            this.styleRowsCount = 0;
            this.currentRow = -1;
            this.currentCell = -1;
            this.divider = 1;
        }

        public SheetParameters(SheetParameters sp) {
            this.firstDataRow = sp.getFirstDataRow();
            this.styleRowsCount = sp.getStyleRowsCount();
            this.currentRow = sp.getCurrentRow();
            this.currentRowStyle = sp.getCurrentRowStyle();
            this.currentRowHeight = sp.getCurrentRowHeight();
            this.currentCell = sp.getCurrentCell();
            this.sumColumns = new ArrayList<>(sp.getSumColumns());
            this.countWBMergedRegions = sp.getCountWBMergedRegions();
            this.sumFormat = sp.getSumFormat();
            this.divider = sp.getDivider();
        }

        public void setSheet(Sheet sheet) {
            this.sheet = sheet;
            setSumColumnsFormat();
        }

        public void setCountWBMergedregions(int count) {
            this.countWBMergedRegions = count;
        }

        public List<Sheet> getSheetQueue() {
            if (this.sheetQueue == null) {
                this.sheetQueue = new ArrayList<>();
                this.sheetQueue.add(sheet);
            }

            return this.sheetQueue;
        }

        public void setSumColumnsFormat() {
            if (this.sheet == null) {
                return;
            }

            for (var i = 0; i < styleRowsCount; i++) {
                var row = firstDataRow + i;
                for (var column : sumColumns) {
                    var cell = this.sheet.getRow(row).getCell(column);
                    if (cell != null) {
                        var format = workbook.createDataFormat();
                        cell.getCellStyle().setDataFormat(format.getFormat(sumFormat));
                    }
                }
            }
        }
    }

    @NoArgsConstructor
    protected static class MatrixRow extends ArrayList<Object> {
        public MatrixRow(Collection<Object> values) {
            super(values);
        }

        public <T> MatrixRow addRange(Collection<T> values) {
            super.addAll(values);
            return this;
        }

        public MatrixRow addVal(Object value) {
            add(value);
            return this;
        }

        public MatrixRow add(Object... values) {
            super.addAll(List.of(values));
            return this;
        }
    }

    protected class Matrix extends PastedObject {
        @Getter
        private List<MatrixRow> store;

        public Matrix() {
            store = new ArrayList<>();
        }

        private Integer height;

        public Integer getHeight() {
            return getStore().size();
        }

        private Integer width;

        public Integer getWidth() {
            return !getStore().isEmpty() ? getStore().size() : 0;
        }

        public Matrix getM(Integer i, Integer j) {
            return (Matrix) getStore().get(i).get(j);
        }

        private MatrixRow sumRow;

        public MatrixRow getSumRow() {
            var sumRow = new MatrixRow();
            for (var row : getStore()) {
                if (sumRow.size() < row.size()) {
                    sumRow.addRange(ReportHelper.nullList(row.size() - sumRow.size()));
                }

                var count = Math.min(sumRow.size(), row.size());
                for (var i = 0; i < count; i++) {
                    var y = ReportHelper.getDouble(row.get(i));
                    if (y != null) {
                        var val = ReportHelper.getDouble(sumRow.get(i));
                        var sum = ReportHelper.zeroSum(val, y);
                        sumRow.set(i, sum);
                    }
                }
            }

            return sumRow;
        }

        public MatrixRow getMatrixRow(int i) {
            return getStore().get(i);
        }

        public MatrixRow addRow(Object... values) {
            getStore().add(new MatrixRow());
            return getStore().get(getStore().size() - 1).addRange(List.of(values));
        }

        public MatrixRow insertRow(int index, Object... values) {
            store.add(index, new MatrixRow());
            return store.get(index).addRange(List.of(values));
        }

        public Matrix addColumn(ArrayList<?> values) {
            justify(values.size() - getHeight());
            if (getHeight() == 0) {
                addRow();
            }

            for (var i = 0; i < getHeight(); i++) {
                getStore().get(i).add(i < values.size() ? values.get(i) : null);
            }

            return this;
        }

        public Matrix addColumn(Object value) {
            return addColumn(Collections.singletonList(value));
        }

        /**
         * Добавляет к матрице соответствующие элементы другой матрицы,
         * увеличивая её размер при необходимости.
         * Предполагается, что элементы имеют тип decimal?
         *
         * @param m Добавляемая матрица
         */
        public Matrix addMatrix(Matrix m) {
            for (var n = 0; n < m.getHeight(); n++) {
                if (n >= getStore().size()) {
                    getStore().add(new MatrixRow(m.getMatrixRow(n)));
                    continue;
                }

                var row = store.get(n);
                for (var i = 0; i < m.getWidth(); i++) {
                    if (i >= row.size()) {
                        row.add(m.getMatrixRow(n).get(i));
                        continue;
                    }

                    var value = ReportHelper.nullSum(row.get(i), m.getM(n, i));
                    row.set(i, value);
                }
            }

            return this;
        }

        public void justify() {
            justify(0);
        }

        /**
         * Выравнивает матрицу по ширине. Недостающим элементам присваивается значение Null
         *
         * @param furtherRows Количество дополнительных строк
         */
        public void justify(int furtherRows) {
            var count = getWidth();
            var rowList = store.stream().filter(row -> row.size() < count).toList();
            for (var row : rowList) {
                row.addRange(ReportHelper.nullList(count - row.size()));
            }

            for (var i = 0; i < furtherRows; i++) {
                addRow().addRange(ReportHelper.nullList(count));
            }
        }

        /**
         * Расширяет матрицу по ширине. Недостающим элементам присваивается значение Null
         *
         * @param width Требуемая ширина
         */
        public Matrix extend(int width) {
            var row = getHeight() > 0 ? store.get(0) : addRow();
            if (row.size() < width) {
                row.addRange(ReportHelper.nullList(width - row.size()));
            }

            justify();
            return this;
        }

        @Override
        public void paste(BaseExcelReport report) {
            if (!report.addRowLines(getHeight())) return;
            var sp = report.getCurrentParams();
            var rowIndex = sp.getCurrentRow();
            var cellIndex = sp.getCurrentCell();
            var maxCellIndex = cellIndex;

            for (var row : store) {
                for (var value : row) {
                    report.getPastedObject(value).paste(report);
                    maxCellIndex = Math.max(maxCellIndex, sp.getCurrentCell());
                }

                sp.setCurrentRow(sp.getCurrentRow() + 1);
                sp.setCurrentCell(cellIndex);
            }

            sp.setCurrentRow(rowIndex);
            sp.setCurrentCell(maxCellIndex);
        }
    }

    protected class TotalRow extends PastedObject {
        private final Map<Integer, Object> values = new HashMap<>();
        private SheetParameters sp = null;
        private int row = -1;

        public TotalRow() {
            super();
        }

        public void add(TotalRow totalRow) {
            for (var val : totalRow.values.entrySet()) {
                add(val.getKey(), val.getValue());
            }
        }

        public TotalValue add(Object value) {
            return new TotalValue(this, value);
        }

        public void add(int index, Object value) {
            if (!this.values.containsKey(index)) {
                this.values.put(index, null);
            }

            this.values.put(index, ReportHelper.nullSum(this.values.get(index), value));

            if (this.row >= 0) {
                var cellValue = (this.sp.getSumColumns().contains(index) && this.values.get(index) instanceof Double)
                        ? (Double) this.values.get(index) / this.sp.getDivider()
                        : this.values.get(index);
                var cell = this.sp.getSheet().getRow(row).getCell(index);
                setCellValue(cell, cellValue);
            }
        }

        public TotalValue addLast(Object value) {
            return new LastTotalValue(this, value);
        }

        public void addLast(int index, Object value) {
            values.put(index, value);
        }

        // Получить значение ячейки
        public Object value(int index) {
            return values.get(index);
        }

        @Override
        public void paste(BaseExcelReport report) {
            this.sp = report.getCurrentParams();
            this.row = sp.getCurrentRow();
            for (var val : values.entrySet()) {
                var cellValue = (sp.getSumColumns().contains(val.getKey()) && val.getValue() instanceof Double)
                        ? (Double) val.getValue() / sp.getDivider()
                        : val.getValue();
                var cell = sp.getSheet().getRow(row).getCell(val.getKey());
                setCellValue(cell, cellValue);
            }
        }

        @Getter
        @Setter
        public class TotalValue extends PastedObject {
            private TotalRow row;
            private Object value;

            TotalValue(TotalRow row, Object value) {
                this.row = row;
                this.value = value;
            }

            @Override
            public void paste(BaseExcelReport report) {
                if (getValue() instanceof Matrix matrix) {
                    for (var i = 0; i < matrix.getHeight(); i++) {
                        for (var n = 0; n < matrix.getMatrixRow(i).size(); n++) {
                            matrix.getMatrixRow(i).set(n, new TotalValue(getRow(), matrix.getM(i, n)));
                        }
                    }
                } else getRow().add(getCurrentParams().getCurrentCell() + 1, getValue());

                getPastedObject(getValue()).paste(report);
            }
        }

        public class LastTotalValue extends TotalValue {
            LastTotalValue(TotalRow row, Object value) {
                super(row, value);
            }

            @Override
            public void paste(BaseExcelReport report) {
                getRow().addLast(report.getCurrentParams().getCurrentCell() + 1, getValue());
                report.getPastedObject(getValue()).paste(report);
            }
        }
    }

    protected SheetParameters addSheetParams(SheetParameters sp) throws ExportServiceException {
        return addSheetParams(sp, workbook);
    }

    protected SheetParameters addSheetParams(SheetParameters sp, Workbook workbook) throws ExportServiceException {
        if (sp.getSheet() == null) {
            if (workbook.getNumberOfSheets() <= getSheetParams().size()) {
                var bookSize = getSheetParams().size() + 1;
                throw new ExportServiceException(
                        "Не хватает листов в шаблоне отчёта." +
                                " Требуется " + bookSize + "-й лист, а в шаблоне их всего " + workbook.getNumberOfSheets() + ".");
            }

            if (sp.getSheetNum() == null) sp.setSheetNum(0);
            sp.setSheet(workbook.getSheetAt(sp.getSheetNum()));
        }

        if (sp.getCurrentRow() == -1) {
            sp.setCurrentRow(sp.getFirstDataRow() + sp.getStyleRowsCount() - 1);
            sp.setCurrentRowHeight(1);
        }

        sp.setCountWBMergedregions(workbook.getSheetAt(sp.getSheetNum()).getNumMergedRegions());
        if (this.currentParams == null) this.currentParams = sp;
        this.sheetParams.add(sp);
        return sp;
    }

    protected void fillColumnsNumbering(Sheet sheet, int numRowIndex) {
        fillColumnsNumbering(sheet, numRowIndex, 1, 0);
    }

    protected void fillColumnsNumbering(Sheet sheet, int numRowIndex, int lastColumn) {
        fillColumnsNumbering(sheet, numRowIndex, 1, 0, lastColumn);
    }

    /**
     * Заполняет строку нумерации колонок
     */
    protected void fillColumnsNumbering(Sheet sheet, int numRowIndex, int firstColumnNumber, int firstColIndex) {
        var numberingRow = sheet.getRow(numRowIndex);
        var lastColumn = numberingRow.getLastCellNum();
        fillColumnsNumbering(sheet, numRowIndex, firstColumnNumber, firstColIndex, lastColumn);
    }

    /**
     * Заполняет строку нумерации колонок
     */
    protected void fillColumnsNumbering(Sheet sheet, int numRowIndex, int firstColumnNumber, int firstColIndex, int lastColumn) {
        var numberingRow = sheet.getRow(numRowIndex);
        var num = firstColumnNumber;

        for (var i = firstColIndex; i <= lastColumn; i++) {
            var mergeReg = getIndexIfCellIsInMergedCells(sheet, numberingRow.getRowNum(), numberingRow.getCell(i).getColumnIndex());
            if (mergeReg != null) {
                var region = sheet.getMergedRegion(mergeReg);
                if (i != region.getFirstColumn() || sheet.isColumnHidden(i)) {
                    continue;
                }
            }

            numberingRow.getCell(i).setCellValue(num++);
        }
    }

    /**
     * Заполняет строку нумерации колонок
     */
    protected void fillColumnsNumbering(int sheetNumber, int numRowIndex) {
        fillColumnsNumbering(workbook.getSheetAt(sheetNumber), numRowIndex, 1, 0);
    }

    /**
     * Заполняет строку нумерации колонок
     */
    protected void fillColumnsNumbering(int sheetNumber, int numRowIndex, int firstColumnNumber, int firstColIndex) {
        fillColumnsNumbering(workbook.getSheetAt(sheetNumber), numRowIndex, firstColumnNumber, firstColIndex);
    }

    protected Boolean addRowLines(int count) {
        var styleRowIndex = currentParams.firstDataRow + currentParams.currentRowStyle;
        while (getCurrentParams().getCurrentRowHeight() < count) {
            var rowIndex = currentParams.currentRow + currentParams.currentRowHeight;
            copyRowTo(styleRowIndex, rowIndex);

//            getCurrentParams().getSheet().getRow(rowIndex).getCells().Value = null; // todo: могут возникнуть проблемы
            currentParams.currentRowHeight++;
        }

        return true;
    }

    public void copyRange(int beginSourceColumn, int endSourceColumn, int destinationColumn) {
        int columnsNum = endSourceColumn - beginSourceColumn + 1;
        for (int i = 0; i < columnsNum; i++) {
            copyColumn(beginSourceColumn + i, destinationColumn + i);
        }
    }

    public void copyColumn(int sourceColumn, int destColumn) {
        var sheet = currentParams.sheet;
        for (var i = 0; i <= sheet.getLastRowNum(); i++) {
            var sourceCell = getCellByXY((XSSFSheet) sheet, i, sourceColumn);
            var destCell = getCellByXY((XSSFSheet) sheet, i, destColumn);
            if (destCell == null && sourceCell != null) {
                destCell = (XSSFCell) sheet.getRow(i).createCell(destColumn);
            }

            if (sourceCell == null) continue;
            copyCell(sourceCell, destCell);
        }

        sheet.setColumnWidth(destColumn, sheet.getColumnWidth(sourceColumn));
    }

    public void copyCell(Cell oldCell, Cell newCell) {
        copyCell(oldCell, newCell, true);
    }

    public void copyCell(Cell oldCell, Cell newCell, boolean setValue) {
        if (oldCell == null) return;
        if (newCell == null) return;

        if (oldCell.getCellComment() != null) newCell.setCellComment(oldCell.getCellComment());
        if (oldCell.getHyperlink() != null) newCell.setHyperlink(oldCell.getHyperlink());

        switch (oldCell.getCellType()) {
            case BLANK, STRING -> {
                if (setValue) newCell.setCellValue(oldCell.getStringCellValue());
            }
            case BOOLEAN -> {
                if (setValue) newCell.setCellValue(oldCell.getBooleanCellValue());
            }
            case ERROR -> {
                if (setValue) newCell.setCellErrorValue(oldCell.getErrorCellValue());
            }
            case FORMULA -> setFormulaValue(oldCell, newCell, setValue);
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(oldCell)) {
                    if (setValue) newCell.setCellValue(oldCell.getDateCellValue());
                } else {
                    DataFormatter dataFormatter = new DataFormatter();
                    var format = dataFormatter.getDefaultFormat(oldCell);
                    dataFormatter.setDefaultNumberFormat(format);
                    if (setValue) newCell.setCellValue(dataFormatter.formatCellValue(newCell));
                }
            }
        }
        newCell.setCellStyle(oldCell.getCellStyle());
    }

    protected void setFormulaValue(Cell oldCell, Cell newCell, boolean setValue) {
        if (setValue) {
            newCell.setCellValue(oldCell.getCellFormula());
            newCell.setCellFormula(oldCell.getCellFormula());
        }
    }

    public XSSFCell getCellByXY(XSSFSheet sheet, int rowIndex, int colIndex) {
        XSSFRow row = sheet.getRow(rowIndex);
        if (row != null) return row.getCell(colIndex);
        return null;
    }

    protected void copyRowTo(Sheet sheet, Row sourceRow, Row newRow) {
        copyRowTo(sheet, sourceRow, newRow, true);
    }

    protected void copyRowTo(Sheet sheet, Row sourceRow, Row newRow, boolean setValue) {
        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            var oldCell = sourceRow.getCell(i);
            if (oldCell == null) continue;
            var newCell = newRow.createCell(i);
            copyCell(oldCell, newCell, setValue);
        }

        deleteMergedRegions(sheet, newRow);
        for (int i = 0; i < currentParams.getCountWBMergedRegions(); i++) {
            var cellRangeAddress = sheet.getMergedRegion(i);
            if (cellRangeAddress.getFirstRow() == sourceRow.getRowNum()) {
                var newCellRangeAddress = new CellRangeAddress(newRow.getRowNum(),
                        (newRow.getRowNum() + (cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow())),
                        cellRangeAddress.getFirstColumn(),
                        cellRangeAddress.getLastColumn());
                if (!sheet.getMergedRegions().contains(newCellRangeAddress)) {
                    sheet.addMergedRegion(newCellRangeAddress);
                }
            }
        }
    }

    private void deleteMergedRegions(Sheet sheet, Row newRow) {
        List<Integer> regionsToRemove = new ArrayList<>();
        for (int i = 0; i < currentParams.getCountWBMergedRegions(); i++) {
            var cellRangeAddress = sheet.getMergedRegion(i);
            if (cellRangeAddress.getFirstRow() == newRow.getRowNum()) {
                regionsToRemove.add(i);
            }
        }
        sheet.removeMergedRegions(regionsToRemove);
    }

    private void copyRowTo(int sourceRowNum, int destinationRowNum) {
        var sheet = currentParams.sheet;
        var newRow = sheet.getRow(destinationRowNum);
        var sourceRow = sheet.getRow(sourceRowNum);
        if (newRow != null) sheet.shiftRows(destinationRowNum, sheet.getLastRowNum(), 1);
        else newRow = sheet.createRow(destinationRowNum);
        copyRowTo(sheet, sourceRow, newRow);
    }

    private static boolean IsNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public void setCellValue(int sheetInd, int row, int column, Object value) {
        var sheet = workbook.getSheetAt(sheetInd);
        var cell = getOrCreateCell(sheet, row, column);
        setCellValue(cell, value);
    }

    public void setCellValueWithStyle(int sheetInd, int row, int column, Object value, CellStyle cellStyle) {
        var sheet = workbook.getSheetAt(sheetInd);
        var cell = getOrCreateCell(sheet, row, column);
        cell.setCellStyle(cellStyle);
        setCellValue(cell, value);
    }

    public void setCellValue(Cell cell, Object cellValue) {
        if (cell == null) return;
        if (cellValue instanceof Integer) cell.setCellValue((Integer) cellValue);
        else if (cellValue instanceof Long) cell.setCellValue((Long) cellValue);
        else if (cellValue instanceof Float) cell.setCellValue((Float) cellValue);
        else if (cellValue instanceof Double) cell.setCellValue((Double) cellValue);
        else if (cellValue instanceof String) cell.setCellValue((String) cellValue);
        else if (cellValue instanceof java.sql.Timestamp) cell.setCellValue((java.sql.Timestamp) cellValue);
        else if (cellValue instanceof java.sql.Date) cell.setCellValue((java.sql.Date) cellValue);
        else if (cellValue instanceof Date) cell.setCellValue((Date) cellValue);
        else if (cellValue instanceof LocalDate) cell.setCellValue((LocalDate) cellValue);
        else if (cellValue instanceof Calendar) cell.setCellValue((Calendar) cellValue);
        else if (cellValue instanceof RichTextString) cell.setCellValue((RichTextString) cellValue);
        else if (cellValue instanceof Boolean) cell.setCellValue((boolean) cellValue);
    }

    public Object getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING -> {
                return cell.getStringCellValue();
            }
            case FORMULA -> {
                return cell.getCellFormula();
            }
            case NUMERIC -> {
                Object cellValue;
                if (DateUtil.isCellDateFormatted(cell)) {
                    cellValue = cell.getDateCellValue();
                } else {
                    cellValue = Double.toString(cell.getNumericCellValue());
                }
                return cellValue;
            }
            case BOOLEAN -> {
                return Boolean.toString(cell.getBooleanCellValue());
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Устанавливаем авто-высоту строк
     */
    public void setAutoHeightCurrentRow() {
        currentParams.getSheet().getRow(currentParams.getCurrentRow()).setHeight((short) -1);
    }

    /**
     * Проверяем, является ли текущая ячейка частью объединённых ячеек, если да - пропускаем все объединенные ячейки
     */
    private void skipMergeRegion(Cell cell) {
        if (cell == null) return;
        currentParams.sheet.getMergedRegions().stream()
                .filter(x -> x.isInRange(cell))
                .findFirst()
                .ifPresent(mergedRegion -> currentParams.currentCell += mergedRegion.getNumberOfCells() - 1);
    }

    private float computeRowHeightInPoints(Sheet sheet, int fontSizeInPoints, int numLines) {
        // Приблизительно то что делает эксель
        var lineHeightInPoints = 1.3f * fontSizeInPoints;
        var rowHeightInPoints = lineHeightInPoints * numLines;
        rowHeightInPoints = Math.round(rowHeightInPoints * 4) / 4f; // округляем до 1/4

        // Не сжимаем строки, чтобы они соответствовали размеру шрифта, а только увеличиваем их
        var defaultRowHeightInPoints = sheet.getDefaultRowHeightInPoints();
        if (rowHeightInPoints < defaultRowHeightInPoints + 1) {
            rowHeightInPoints = defaultRowHeightInPoints;
        }
        return rowHeightInPoints;
    }

    /**
     * Возвращает список колонок, для которых заполнена нумерация
     */
    protected List<Integer> getDataColumns(int columnNumberingRow) {
        List<Integer> cells = new ArrayList<>();
        List<Cell> cells1 = new ArrayList<>();
        var row = currentParams.getSheet().getRow(columnNumberingRow);
        for (var cell : row) {
            if (getCellValue(cell) != null && getCellValue(cell) != "") {
                cells.add(cell.getColumnIndex());
                cells1.add(cell);
            }
        }
        return cells;
    }

    private boolean isNullOrBlank(String param) {
        return param == null || param.trim().isEmpty();
    }

    private Integer getIndexIfCellIsInMergedCells(Sheet sheet, int row, int column) {
        var numberOfMergedRegions = sheet.getNumMergedRegions();
        for (int i = 0; i < numberOfMergedRegions; i++) {
            var mergedCell = sheet.getMergedRegion(i);
            if (mergedCell.isInRange(row, column)) {
                return i;
            }
        }

        return null;
    }
}
