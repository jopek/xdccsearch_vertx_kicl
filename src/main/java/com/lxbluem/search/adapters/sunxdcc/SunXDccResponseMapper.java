package com.lxbluem.search.adapters.sunxdcc;

import com.lxbluem.search.domain.ports.SearchGateway;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class SunXDccResponseMapper {
    private static final Pattern filesizePattern = Pattern.compile("\\[\\s*([\\d.]+)(\\w)\\]");
    private static final Pattern networNamePattern = Pattern.compile("(?:\\w*\\.)?([\\w-]+)\\.\\w+");

    public static SearchGateway.SearchResponse mapToSearchResponse(
            SunXDccResponse sunXDccResponse,
            int pageNumber) {

        int resultCount = sunXDccResponse.getBot().size();
        boolean hasMore = resultCount == 50;
        List<SearchGateway.ResponsePack> results = IntStream.range(0, resultCount)
                .boxed()
                .map(idx -> mapToResponsePack(sunXDccResponse, idx))
                .collect(toList());

        return SearchGateway.SearchResponse.builder()
                .hasMore(hasMore)
                .currentPage(pageNumber)
                .results(results)
                .build();
    }

    private static SearchGateway.ResponsePack mapToResponsePack(SunXDccResponse sunXDccResponse, Integer idx) {
        {
            String packNum = sunXDccResponse.getPacknum().get(idx);
            int packNumber = Integer.parseInt(packNum.replace("#", ""));
            String bot = sunXDccResponse.getBot().get(idx);
            String network = sunXDccResponse.getNetwork().get(idx);
            Matcher networkNameMatcher = networNamePattern.matcher(network);
            String networkName = (networkNameMatcher.matches()) ? networkNameMatcher.group(1) : "";

            String channelName = sunXDccResponse.getChannel().get(idx);
            String fsize = sunXDccResponse.getFsize().get(idx);
            String fname = sunXDccResponse.getFname().get(idx);
            String sgets = sunXDccResponse.getGets().get(idx);
            int gets = Integer.parseInt(sgets.replace("x", ""));

            long approxFileSize = textRepresentationToBytes(fsize);

            return SearchGateway.ResponsePack.builder()
                    .networkName(StringUtils.capitalize(networkName))
                    .serverHostName(network)
                    .serverPort(6667)
                    .channelName(channelName)
                    .sizeBytes(approxFileSize)
                    .nickName(bot)
                    .packNumber(packNumber)
                    .packName(fname)
                    .packGets(gets)
                    .last(0)
                    .age(0)
                    .build();
        }
    }


    static long textRepresentationToBytes(String fsize) {
        Matcher matcher = filesizePattern.matcher(fsize);
        if (matcher.find()) {
            int amount = Math.round(Float.parseFloat(matcher.group(1)) * 10);
            long factor = 1;

            switch (matcher.group(2).toLowerCase()) {
                case "k":
                    factor = 1 << 10;
                    break;
                case "m":
                    factor = 1 << 20;
                    break;
                case "g":
                    factor = 1 << 30;
                    break;
            }
            return (long) Math.floor(amount * factor / 10.);

        }
        return 0;
    }
}
