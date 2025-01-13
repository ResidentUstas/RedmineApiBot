package ru.krista.fm.redmine.helpers;

import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.krista.fm.redmine.interfaces.Report;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@NoArgsConstructor
public class RestHelper {

    public static MultiValueMap<String, String> processHeaders(Map<String, String> headers) {
        var allowOrigin = headers.getOrDefault("Origin", null);
        MultiValueMap<String, String> outHeaders = new LinkedMultiValueMap<>();
        outHeaders.add("Access-Control-Allow-Headers", "Content-Type");
        if (allowOrigin != null && !allowOrigin.isEmpty()) outHeaders.add("Access-Control-Allow-Origin", allowOrigin);
        else outHeaders.add("Access-Control-Allow-Origin", "*");
        return outHeaders;
    }

    public static void addContentDispositionHeader(MultiValueMap<String, String> outHeaders, String fileName) {
        outHeaders.add(
                "Content-Disposition",
                "attachment;filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20") + "\"");
    }

    public static ResponseEntity<Resource> getResultResourceResponse(Map<String, String> headers, Report.ReportResult reportResult) {
        var outHeaders = processHeaders(headers);
        addContentDispositionHeader(outHeaders, reportResult.getFileName());
        return ResponseEntity
                .ok()
                .headers(new HttpHeaders(outHeaders))
                .contentType(MediaType.parseMediaType(reportResult.getMimeType()))
                .body(reportResult.fileBody);
    }

    public static ResponseEntity<Resource> getResultResourceResponse(Map<String, String> headers, String fileName, String contentType, Resource resource) {
        var outHeaders = processHeaders(headers);
        addContentDispositionHeader(outHeaders, fileName);
        return ResponseEntity
                .ok()
                .headers(new HttpHeaders(outHeaders))
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    public static ResponseEntity<Double> getResultProgressResponse(Map<String, String> headers, Double percent) {
        var outHeaders = processHeaders(headers);
        addContentDispositionHeader(outHeaders, "percent");
        return ResponseEntity
                .ok()
                .headers(new HttpHeaders(outHeaders))
                .contentType(MediaType.valueOf("application/json"))
                .body(percent);
    }
}
