package com.alexlego19.ainpcs.data;

public class NPCProfile {
    private String id;
    private String name;
    private String skinId;
    private boolean isSlim;
    private boolean isEnabled;

    public NPCProfile(String id, String name, String skinId, boolean isSlim, boolean isEnabled) {
        this.id = id;
        this.name = name;
        this.skinId = skinId;
        this.isSlim = isSlim;
        this.isEnabled = isEnabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSkinId() { return skinId; }
    public void setSkinId(String skinId) { this.skinId = skinId; }
    public boolean isSlim() { return isSlim; }
    public void setSlim(boolean slim) { isSlim = slim; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
}
