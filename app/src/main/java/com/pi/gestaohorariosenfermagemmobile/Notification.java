package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class Notification {
    private int id;
    private String subject;
    private String body;
    private boolean read;
    @SerializedName("created_at")
    private String created_at;

    public int getId() { return id; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public boolean isRead() { return read; }
    public String getCreatedAt() { return created_at; }

    public void setRead(boolean read) { this.read = read; }
}
