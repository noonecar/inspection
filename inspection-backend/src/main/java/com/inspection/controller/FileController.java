package com.inspection.controller;

import com.inspection.common.result.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(name = "category", defaultValue = "evidence") String category) throws IOException {
        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file.dat";
        String extension = "";
        int dotIndex = original.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = original.substring(dotIndex);
        }
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().replace("-", "") + extension;

        Path dir = Paths.get(uploadDir, category);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String relativeUrl = "/uploads/" + category + "/" + fileName;
        Map<String, String> data = new HashMap<>();
        data.put("url", relativeUrl);
        data.put("name", original);
        return ApiResponse.ok("上传成功", data);
    }

    @GetMapping("/view/{category}/{filename:.+}")
    public void viewFile(@PathVariable("category") String category,
                          @PathVariable("filename") String filename,
                          HttpServletResponse response) throws IOException {
        Path file = Paths.get(uploadDir, category, filename);
        if (!Files.exists(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        Files.copy(file, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
