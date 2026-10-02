/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.model.DocumentRecord;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.*;
import java.util.*;

public class DocumentDAO {

    public static final List<String> MANDATORY_DOCS = Arrays.asList(
        "South African ID", 
        "Matric Certificate or Latest Results", 
        "Proof of Registration", 
        "Proof of Income"
    );

    public boolean hasAllRequiredDocuments(int userId) throws SQLException {
        Set<String> verifiedTypes = new HashSet<>();
        String sql = "SELECT document_type FROM user_documents WHERE user_id = ? AND verification_status = 'Verified'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    verifiedTypes.add(rs.getString("document_type"));
                }
            }
        }
        return verifiedTypes.containsAll(MANDATORY_DOCS);
    }

    public List<DocumentRecord> getUserDocuments(int userId) throws SQLException {
        List<DocumentRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM user_documents WHERE user_id = ? ORDER BY uploaded_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentRecord doc = new DocumentRecord();
                    doc.setDocumentId(rs.getInt("document_id"));
                    doc.setUserId(userId);
                    doc.setDocumentType(rs.getString("document_type"));
                    doc.setFileName(rs.getString("file_name"));
                    doc.setFilePath(rs.getString("file_path"));
                    doc.setFileSizeBytes(rs.getInt("file_size_bytes"));
                    doc.setVerificationStatus(rs.getString("verification_status"));
                    doc.setUploadedAt(rs.getTimestamp("uploaded_at"));
                    list.add(doc);
                }
            }
        }
        return list;
    }

    public void addDocument(DocumentRecord d) throws SQLException {
        String sql = "INSERT INTO user_documents (user_id, document_type, file_name, file_path, file_size_bytes, verification_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getUserId());
            ps.setString(2, d.getDocumentType());
            ps.setString(3, d.getFileName());
            ps.setString(4, d.getFilePath());
            ps.setInt(5, d.getFileSizeBytes());
            ps.setString(6, d.getVerificationStatus());
            ps.executeUpdate();
        }
    }

    public boolean deleteDocument(int userId, int documentId) throws SQLException {
        String sql = "DELETE FROM user_documents WHERE document_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}