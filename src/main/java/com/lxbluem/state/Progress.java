package com.lxbluem.state;

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
