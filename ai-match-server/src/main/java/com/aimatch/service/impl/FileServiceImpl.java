package com.aimatch.service.impl;

import com.aimatch.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {".pdf", ".doc", ".docx"};
    private static final String[] ALLOWED_MIME_TYPES = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String type) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过20MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        boolean allowed = false;
        String lower = originalFilename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new RuntimeException("仅支持Word(.doc/.docx)和PDF格式的文件");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fileName", originalFilename);
        result.put("fileSize", file.getSize());
        result.put("fileType", getFileExtension(originalFilename));
        return result;
    }

    @Override
    public String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, new org.apache.tika.metadata.Metadata(), context);
            return handler.toString();
        } catch (TikaException e) {
            log.error("文件解析异常(文档可能受保护或加密)", e);
            throw new RuntimeException("文件解析失败，请确认文件未被加密或损坏");
        } catch (Exception e) {
            log.error("文件读取失败", e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "unknown";
    }
}
