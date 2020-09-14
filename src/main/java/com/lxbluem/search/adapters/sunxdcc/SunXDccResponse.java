package com.lxbluem.search.adapters.sunxdcc;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SunXDccResponse {
    private List<String> botrec;
    private List<String> network;
    private List<String> bot;
    private List<String> channel;
    private List<String> packnum;
    private List<String> gets;
    private List<String> fsize;
    private List<String> fname;

}
