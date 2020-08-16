package com.lxbluem.irc.adapter;

import com.lxbluem.domain.Pack;
import com.lxbluem.irc.usecase.ports.PackStorage;

import java.util.HashMap;
import java.util.Map;

public class InMemoryPackStorage implements PackStorage {
    private final Map<String, Pack> packs = new HashMap<>();

    @Override
    public void save(String botNick, Pack pack) {
        packs.put(botNick, pack);
    }

    @Override
    public Pack getPackByNick(String botNick) {
        return packs.get(botNick);
    }

}
