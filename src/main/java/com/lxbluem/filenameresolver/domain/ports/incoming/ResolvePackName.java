package com.lxbluem.filenameresolver.domain.ports.incoming;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import rx.Single;

public interface ResolvePackName {
    Single<Response> execute(String requestedFilename, long requestedFileSize);

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    class Response {
        private final String filename;
        private final long filesize;
        private final boolean isComplete;
    }
}
