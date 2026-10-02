/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.model;

public class Lesson {
    private int lessonId;
    private String title;
    private String category;
    private int durationMin;
    private int xpReward;
    private String videoTimestamp;
    private String whyItMatters;
    private String plainLanguageBody;
    private boolean completed;

    public Lesson() {}

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }
    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    public String getVideoTimestamp() { return videoTimestamp; }
    public void setVideoTimestamp(String videoTimestamp) { this.videoTimestamp = videoTimestamp; }
    public String getWhyItMatters() { return whyItMatters; }
    public void setWhyItMatters(String whyItMatters) { this.whyItMatters = whyItMatters; }
    public String getPlainLanguageBody() { return plainLanguageBody; }
    public void setPlainLanguageBody(String plainLanguageBody) { this.plainLanguageBody = plainLanguageBody; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}