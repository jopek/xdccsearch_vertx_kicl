package com.lxbluem.search.adapters.sunxdcc;

import com.lxbluem.search.domain.ports.SearchGateway;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class SunXDccSearchGatewayResponseMapperTest {

    JsonObject INPUT = new JsonObject()
            .put("botrec", new JsonArray(asList(
                    "106361.7kB/s",
                    "152468.7kB/s"
            )))
            .put("network", new JsonArray(asList(
                    "irc.abjects.net",
                    "irc.xeromem.com"
            )))
            .put("bot", new JsonArray(asList(
                    "[MG]-X265|EU|S|Maryjane",
                    "TheTopCat"
            )))
            .put("channel", new JsonArray(asList(
                    "#moviegods",
                    "#CRACKERJACK"
            )))
            .put("packnum", new JsonArray(asList(
                    "#117",
                    "#47"
            )))
            .put("gets", new JsonArray(asList(
                    "248x",
                    "7x"
            )))
            .put("fsize", new JsonArray(asList(
                    "[ 18G]",
                    "[3.9G]"
            )))
            .put("fname", new JsonArray(asList(
                    "Terminator.Dark.Fate.2019.German.DTS.DL.2160p.UHD.BluRay.HDR.x265-NIMA4K.mkv",
                    "Terminator.Dark.Fate.2019.1080p.WEBRip.x264.AC3-RPG.mkv"
            )));

    @Test
    public void mapToPack() {
        SunXDccResponse sunXDccResponse = INPUT.mapTo(SunXDccResponse.class);

        SearchGateway.SearchResponse mapped = SunXDccResponseMapper.mapToSearchResponse(sunXDccResponse, 3);

        SearchGateway.SearchResponse expected = SearchGateway.SearchResponse.builder()
                .currentPage(3)
                .hasMore(false)
                .results(asList(
                        SearchGateway.ResponsePack.builder()
                                .networkName("Abjects")
                                .serverHostName("irc.abjects.net")
                                .serverPort(6667)
                                .channelName("#moviegods")
                                .nickName("[MG]-X265|EU|S|Maryjane")
                                .packName("Terminator.Dark.Fate.2019.German.DTS.DL.2160p.UHD.BluRay.HDR.x265-NIMA4K.mkv")
                                .sizeBytes(18 * 1073741824L)
                                .packNumber(117)
                                .packGets(248)
                                .build(),

                        SearchGateway.ResponsePack.builder()
                                .networkName("Xeromem")
                                .serverHostName("irc.xeromem.com")
                                .serverPort(6667)
                                .channelName("#CRACKERJACK")
                                .nickName("TheTopCat")
                                .packName("Terminator.Dark.Fate.2019.1080p.WEBRip.x264.AC3-RPG.mkv")
                                .sizeBytes((long) (3.9 * (1 << 30)))
                                .packNumber(47)
                                .packGets(7)
                                .build()
                )).build();

        assertEquals(expected, mapped);
    }

    @Test
    public void filesize_mapping() {
        assertEquals(2048, SunXDccResponseMapper.textRepresentationToBytes("[  2K]"));
        assertEquals(1 << 20, SunXDccResponseMapper.textRepresentationToBytes("[  1M]"));
        assertEquals(3.5 * (1 << 20), SunXDccResponseMapper.textRepresentationToBytes("[3.5M]"), 0.1);
        assertEquals(215 * (1 << 20), SunXDccResponseMapper.textRepresentationToBytes("[215M]"), 0.1);
    }
}