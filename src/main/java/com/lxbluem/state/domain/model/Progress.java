package com.lxbluem.state.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Progress {
    public long size;
    public long time;

    public void setProgress(Progress other) {
        size = other.size;
        time = other.time;
    }
}
