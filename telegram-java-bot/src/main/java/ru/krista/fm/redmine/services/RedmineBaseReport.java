package ru.krista.fm.redmine.services;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import ru.krista.fm.redmine.exceptions.ExportServiceException;

import java.io.IOException;

public abstract class RedmineBaseReport extends BaseExcelReport {

    public RedmineBaseReport() {
    }

    @Override
    public ReportResult generateReport(ParameterRec[] parameters)
            throws Exception {
        setWorkbook();
        if (setup(parameters)) fillReportData();
        complete();
        return new ReportResult(getResultStream(), getFileName(), getMimeType());
    }

    private void setWorkbook() throws IOException, ExportServiceException {
        var classLoader = getClass().getClassLoader();
        var file = classLoader.getResourceAsStream("templates/redmine/report.xlsx");
        assert file != null;
        workbook = WorkbookFactory.create(file);
        setupSheetParams();
    }

    protected void setupSheetParams() throws ExportServiceException {
        addSheetParams(new SheetParameters() {{
            setSheetNum(0);
            setFirstDataRow(3 - 1);
            setStyleRowsCount(10);
        }});
    }
}
