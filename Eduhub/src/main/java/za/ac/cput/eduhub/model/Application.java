/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.model;
import java.sql.Timestamp;
/**
 *
 * @author Comaro Crossing
 */


public class Application {
    private int applicationId;
    private int userId;
    private int bursaryId;
    private String bursaryName;
    private String status;
    private Timestamp submittedAt;

    public Application() {}

    public Application(int applicationId, int userId, int bursaryId, String status, Timestamp submittedAt) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.bursaryId = bursaryId;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBursaryId() { return bursaryId; }
    public void setBursaryId(int bursaryId) { this.bursaryId = bursaryId; }

    public String getBursaryName() { return bursaryName; }
    public void setBursaryName(String bursaryName) { this.bursaryName = bursaryName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
}