package ru.krista.fm.redmine.services;

import com.taskadapter.redmineapi.bean.Issue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import ru.krista.fm.redmine.enums.Projects;
import ru.krista.fm.redmine.helpers.DateHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExportService extends RedmineBaseReport {

    private List<Issue> issueList;
    private int index;

    @Override
    protected Boolean setup(ParameterRec[] repParams) {
        issueList = (List<Issue>) repParams[0].getValue();

        setFileName(String.format("отчёт за %s.xlsx", DateHelper.getCurrentMonthName()));
        replacements.put("MOUNTH+1", DateHelper.getCurrentMonthName(1));
        replacements.put("MOUNTH", DateHelper.getCurrentMonthName());
        replacements.put("YEAR+1", DateHelper.getCurrentYear());
        replacements.put("YEAR", DateHelper.getCurrentYear());
        makeReplacement();

        return true;
    }

    @Override
    protected void fillReportData() {
        var finishedIssues = issueList.stream().filter(x -> x.getStatusId() == 3).toList();
        var processIssues = issueList.stream().filter(x -> x.getStatusId() != 3).toList();
        var finishedIssuesGroups = finishedIssues.stream().collect(Collectors.groupingBy(Issue::getProjectId)).entrySet();
        var processIssuesGroups = processIssues.stream().collect(Collectors.groupingBy(Issue::getProjectId)).entrySet();
        var finishKeys = finishedIssuesGroups.stream().map(Map.Entry::getKey).toList();
        var processKeys = processIssuesGroups.stream().map(Map.Entry::getKey).toList();
        index = 1;

        outCompletedIssues(finishKeys, finishedIssuesGroups);
        addRow(8);
        outCompletedIssues(processKeys, processIssuesGroups);
    }

    private void outCompletedIssues(List<Integer> keys, Set<Map.Entry<Integer, List<Issue>>> issuesGroups) {
        //RIA
        if (keys.contains(Projects.RIA.getRedmineProjectId())) {
            printIssuesByGroups(issuesGroups, 0, List.of(Projects.RIA.getRedmineProjectId()));
        }

        //3-звено, воркплейс, СУБД
        if (keys.contains(Projects.Workplace.getRedmineProjectId()) || keys.contains(Projects.Database.getRedmineProjectId())) {
            printIssuesByGroups(issuesGroups, 1, List.of(Projects.Workplace.getRedmineProjectId(), Projects.Database.getRedmineProjectId()));
        }

        //Отчеты
        if (keys.contains(Projects.Reports.getRedmineProjectId())) {
            printIssuesByGroups(issuesGroups, 2, List.of(Projects.Reports.getRedmineProjectId()));
        }

        //Web-cборы
        if (keys.contains(Projects.WEB_fees.getRedmineProjectId())) {
            printIssuesByGroups(issuesGroups, 3, List.of(Projects.WEB_fees.getRedmineProjectId()));
        }

        //Импортозамещение, НТП
        if (keys.contains(Projects.FM_WEB.getRedmineProjectId())) {
            printIssuesByGroups(issuesGroups, 5, List.of(Projects.FM_WEB.getRedmineProjectId()));
        }

        //Автообновление
        if (keys.contains(Projects.AutoUpdateSystem.getRedmineProjectId())) {
            printIssuesByGroups(issuesGroups, 7, List.of(Projects.AutoUpdateSystem.getRedmineProjectId()));
        }
    }

    private void printIssuesByGroups(Set<Map.Entry<Integer, List<Issue>>> issueGroups, int rowStyle, List<Integer> projects) {
        addRow(rowStyle);
        var riaIssues = issueGroups.stream()
                .filter(x -> projects.contains(x.getKey()))
                .map(Map.Entry::getValue).findAny().get();
        for (var issue : riaIssues) {
            addRow(9,
                    index++,
                    issue.getCustomFieldById(1).getValue(),
                    issue.getSubject(),
                    issue.getId(),
                    issue.getTracker().getName().equals("Feature") ||
                            issue.getTracker().getName().equals("Task") ? "Разработка" : "Доработка",
                    issue.getAssigneeName(),
                    issue.getCustomFieldById(6).getValues().get(0)
            );

            setAutoHeightCurrentRow();
        }
    }
}
