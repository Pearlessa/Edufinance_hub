package za.ac.cput.eduhub.controller;

import com.google.gson.Gson;
import za.ac.cput.eduhub.dao.DocumentDAO;
import za.ac.cput.eduhub.model.DocumentRecord;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Handles the Document Vault: listing a student's uploaded documents and
 * accepting new uploads (PDF/JPG/PNG, max 10MB, matching the multipart-config
 * declared for this servlet in web.xml).
 *
 * Mapped via web.xml to /api/documents/* (this class intentionally has no
 * @WebServlet annotation, since combining an annotation with a web.xml
 * <servlet-mapping> for the same URL pattern makes the container refuse to
 * deploy the whole application).
 */
public class DocumentServlet extends HttpServlet {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private final DocumentDAO documentDAO = new DocumentDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        try {
            List<DocumentRecord> records = documentDAO.getUserDocuments(userId);
            boolean hasAll = documentDAO.hasAllRequiredDocuments(userId);

            Set<String> verifiedTypes = new java.util.HashSet<>();
            for (DocumentRecord d : records) {
                if ("Verified".equalsIgnoreCase(d.getVerificationStatus())) {
                    verifiedTypes.add(d.getDocumentType());
                }
            }
            int totalRequired = DocumentDAO.MANDATORY_DOCS.size();
            int verifiedCount = 0;
            for (String required : DocumentDAO.MANDATORY_DOCS) {
                if (verifiedTypes.contains(required)) {
                    verifiedCount++;
                }
            }
            int completionPercentage = totalRequired == 0 ? 0 : (verifiedCount * 100) / totalRequired;

            List<Map<String, Object>> docsOut = new ArrayList<>();
            for (DocumentRecord d : records) {
                Map<String, Object> m = new HashMap<>();
                m.put("documentId", d.getDocumentId());
                m.put("documentType", d.getDocumentType());
                m.put("fileName", d.getFileName());
                m.put("verificationStatus", d.getVerificationStatus());
                m.put("uploadedAt", d.getUploadedAt() != null ? d.getUploadedAt().toString() : null);
                docsOut.add(m);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("documents", docsOut);
            data.put("completionPercentage", completionPercentage);
            data.put("verifiedCount", verifiedCount);
            data.put("totalRequired", totalRequired);
            data.put("allRequiredUploaded", hasAll);
            data.put("mandatoryDocuments", DocumentDAO.MANDATORY_DOCS);

            out.print(gson.toJson(data));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        try {
            Part filePart = req.getPart("file");
            String documentType = req.getParameter("documentType");

            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Please choose a file to upload.\"}");
                return;
            }
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"File exceeds the maximum allowable limit of 10 MB.\"}");
                return;
            }

            String originalName = extractFileName(filePart);
            String extension = getExtension(originalName);
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Only PDF, JPG and PNG files are allowed.\"}");
                return;
            }

            // Store outside the deployed webapp so re-deploys don't wipe uploads.
            Path uploadRoot = Paths.get(System.getProperty("user.home"), ".eduhub_uploads", String.valueOf(userId));
            Files.createDirectories(uploadRoot);
            String storedFileName = System.currentTimeMillis() + "_" + sanitizeFileName(originalName);
            Path destination = uploadRoot.resolve(storedFileName);

            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            DocumentRecord record = new DocumentRecord();
            record.setUserId(userId);
            record.setDocumentType(documentType != null ? documentType.trim() : "Other");
            record.setFileName(originalName);
            record.setFilePath(destination.toString());
            record.setFileSizeBytes((int) filePart.getSize());
            record.setVerificationStatus("Pending"); // reviewed manually before being counted as Verified

            documentDAO.addDocument(record);

            out.print("{\"success\": true, \"message\": \"Document uploaded and pending verification.\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"Upload failed: " + escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        String path = req.getPathInfo(); // expected: /{documentId}
        try {
            if (path == null || path.length() < 2) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Missing document id.\"}");
                return;
            }
            int documentId = Integer.parseInt(path.substring(1));
            boolean deleted = documentDAO.deleteDocument(userId, documentId);
            out.print("{\"success\": " + deleted + "}");
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"Invalid document id.\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String extractFileName(Part part) {
        String header = part.getHeader("content-disposition");
        if (header == null) return "upload";
        for (String token : header.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                String name = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                return slash >= 0 ? name.substring(slash + 1) : name;
            }
        }
        return "upload";
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }

    private int resolveUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s != null && s.getAttribute("userId") != null) ? (int) s.getAttribute("userId") : 1;
    }
}
