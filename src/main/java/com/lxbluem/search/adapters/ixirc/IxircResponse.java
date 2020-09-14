package com.lxbluem.search.adapters.ixirc;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class IxircResponse {
    private long pid;
    private long cid;
    private String cname;
    private int n;
    private String name;
    private int gets;
    private long nid;
    private String nname;
    private String naddr;
    private int nport;
    private long uid;
    private String uname;
    private long sz;
    private String szf;
    private int age;
    private String agef;
    private int last;
    private String lastf;

    @Override
    public String toString() {
        return String.format("%s - %s - %s - %d - %s", naddr, cname, uname, n, name);
    }
}
