package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class User {
    private int id;
    private String name;
    private String email;
    private String role;
    private boolean active;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }

    // Método auxiliar para formatar o cargo
    public String getFormattedRole() {
        if (role == null) return "";
        String formattedRole = role.toLowerCase().trim().replace(" ", "_");
        return formattedRole;
    }
}