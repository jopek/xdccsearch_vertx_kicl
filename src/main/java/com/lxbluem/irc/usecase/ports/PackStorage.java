package com.lxbluem.irc.usecase.ports;


import com.lxbluem.domain.Pack;

public interface PackStorage {
    void save(String botNick, Pack pack);
    Pack getPackByNick(String botNick);
}
