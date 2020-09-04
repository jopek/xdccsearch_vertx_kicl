package com.lxbluem.irc.domain.ports.outgoing;


import com.lxbluem.common.domain.Pack;

public interface PackStorage {
    void save(String botNick, Pack pack);

    Pack getPackByNick(String botNick);
}
