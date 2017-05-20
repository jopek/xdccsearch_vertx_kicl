package com.lxbluem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PackInfo {
    @JsonProperty(value = "pid")
    private long packId;

    @JsonProperty(value = "cname")
    private String channelName;

    // === PACK
    @JsonProperty(value = "n")
    private int packNumber;

    @JsonProperty(value = "name")
    private String packName;

    @JsonProperty(value = "gets")
    private int packGets;

    @JsonProperty(value = "nname")
    private String networkName;

    @JsonProperty(value = "naddr")
    private String serverHostName;

    @JsonProperty(value = "nport")
    private int serverPort;

    @JsonProperty(value = "uname")
    private String nickName;

    // === SIZE
    @JsonProperty(value = "sz")
    private long sizeBytes;

    @JsonProperty(value = "szf")
    private String sizeFormatted;

    // === AGE
    @JsonProperty(value = "age")
    private int age;

    @JsonProperty(value = "agef")
    private String ageFormatted;

    @JsonProperty(value = "last")
    private int lastAdvertised;

    @JsonProperty(value = "lastf")
    private String lastAdvertisedFormatted;

    @Override
    public String toString() {
        return String.format("%s - %s - %s - %d - %s", serverHostName, channelName, nickName, packNumber, packName);
    }

    @JsonIgnore
    public boolean isNickNameMissing() {
        return nickName == null || nickName.isEmpty();
    }
}
