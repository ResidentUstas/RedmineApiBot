package ru.krista.fm.redmine.services;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.Journal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.krista.fm.redmine.enums.Projects;
import ru.krista.fm.redmine.helpers.DateHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExportService extends RedmineBaseReport {
    private final RedmineService redmineService;
    private List<Issue> issueList;
    private int index;
    private boolean isPlan;
    private LocalDate date;
    private long userId;

    public ExportService(RedmineService redmineService) {
        this.redmineService = redmineService;
    }

    @Override
    protected Boolean setup(ParameterRec[] repParams) {
        issueList = (List<Issue>) repParams[0].getValue();
        date = (LocalDate) repParams[1].getValue();
        userId = Long.parseLong(String.valueOf(repParams[2].getValue()));

        setFileName(String.format("%s_отчёт за %s.xlsx", !issueList.isEmpty() ?
                issueList.get(0).getAssigneeName() + "_" : "", DateHelper.getMonthName(date.getMonthValue(), false)));
        replacements.put("MOUNTH+1", DateHelper.getCurrentMonthName(1));
        replacements.put("MOUNTH", DateHelper.getCurrentMonthName());
        replacements.put("YEAR+1", DateHelper.getCurrentYear());
        replacements.put("YEAR", DateHelper.getCurrentYear());
        makeReplacement();

        return true;
    }

    @Override
    protected void fillReportData() throws RedmineException {
        // Получаем решённые задачи задачи (статус = решена)
        var doneIssues = new ArrayList<>(issueList.stream().filter(x -> x.getDoneRatio() == 100).toList());

        // Проверяем по историй за прощедший месяц, ставился ли задаче статус решённой её исполнителем. Неподходящие условию задачи отбрасываем
        var filterDoneIssues = checkProcessIssues(doneIssues);
        var processIssues = new ArrayList<>(issueList.stream().filter(x -> x.getDoneRatio() != 100).toList());

        // Проверяем по историй за прощедший месяц, ставился ли задаче статус решённой её исполнителем.
        var filterProcessIssues = checkProcessIssues(processIssues);
        processIssues.removeAll(filterProcessIssues);
        filterDoneIssues.addAll(filterProcessIssues);
        var finishedIssuesGroups = filterDoneIssues.stream().collect(Collectors.groupingBy(Issue::getProjectId)).entrySet();
        var processIssuesGroups = processIssues.stream().collect(Collectors.groupingBy(Issue::getProjectId)).entrySet();
        var finishKeys = finishedIssuesGroups.stream().map(Map.Entry::getKey).toList();
        var processKeys = processIssuesGroups.stream().map(Map.Entry::getKey).toList();
        index = 1;

        isPlan = false;
        outCompletedIssues(finishKeys, finishedIssuesGroups);
        addRow(8);
        isPlan = true;
        index = 1;
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
                    getTrackerName(issue.getTracker().getName()),
                    isPlan ? "" : issue.getAssigneeName(),
                    !issue.getCustomFieldById(6).getValues().isEmpty() ?
                            issue.getCustomFieldById(6).getValues().get(0) : ""
            );

            setAutoHeightCurrentRow();
        }
    }

    private List<Issue> checkProcessIssues(List<Issue> processIssues) throws RedmineException {
        List<Issue> result = new ArrayList<>();
        for (var issue : processIssues) {
            var issueJournals = redmineService.getIssueJournal(issue.getId()).stream().filter(x -> {
                try {
                    return x.getCreatedOn().getTime() < getBorderMonth(true).getTime() &&
                            x.getCreatedOn().getTime() > getBorderMonth(false).getTime() && x.getUser().getId() == userId;
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
            var details = issueJournals.stream().flatMap(x -> x.getDetails().stream()).toList();
            var checkOP = details.stream().anyMatch(x -> x.getName().equals("done_ratio") &&
                    x.getNewValue().equals("100"));
            if (checkOP) {
                result.add(issue);
            }
        }
        return result;
    }

    private Date getBorderMonth(boolean isEnd) throws ParseException {
        LocalDate startOfMonth = YearMonth.from(date).atDay(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        return isEnd ? sdf.parse(endOfMonth.toString()) : sdf.parse(startOfMonth.toString());
    }

    private String getTrackerName(String name) {
        switch (name) {
            case "Feature":
                return "Доработка (chg)";
            case "Task":
                return "Разработка (add)";
            case "Bug":
                return "Исправление ошибок (fix)";
            case "Patch":
                return "Доработка (chg), Рефакторинг (refact)";
        }

        return "Доработка (chg)";
    }
}
