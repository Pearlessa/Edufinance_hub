/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.model;

import java.sql.Date;

public class Bursary {
    private int bursaryId;
    private String title;
    private String organization;
    private String category;
    private String fundingAmountDesc;
    private Date closingDeadline;
    private double minAcademicPercentage;
    private double maxHouseholdIncome;
    private String targetField;
    private String aboutText;
    private boolean active;

    public Bursary() {}

    public int getBursaryId() { return bursaryId; }
    public void setBursaryId(int bursaryId) { this.bursaryId = bursaryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFundingAmountDesc() { return fundingAmountDesc; }
    public void setFundingAmountDesc(String fundingAmountDesc) { this.fundingAmountDesc = fundingAmountDesc; }
    public Date getClosingDeadline() { return closingDeadline; }
    public void setClosingDeadline(Date closingDeadline) { this.closingDeadline = closingDeadline; }
    public double getMinAcademicPercentage() { return minAcademicPercentage; }
    public void setMinAcademicPercentage(double minAcademicPercentage) { this.minAcademicPercentage = minAcademicPercentage; }
    public double getMaxHouseholdIncome() { return maxHouseholdIncome; }
    public void setMaxHouseholdIncome(double maxHouseholdIncome) { this.maxHouseholdIncome = maxHouseholdIncome; }
    public String getTargetField() { return targetField; }
    public void setTargetField(String targetField) { this.targetField = targetField; }
    public String getAboutText() { return aboutText; }
    public void setAboutText(String aboutText) { this.aboutText = aboutText; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}