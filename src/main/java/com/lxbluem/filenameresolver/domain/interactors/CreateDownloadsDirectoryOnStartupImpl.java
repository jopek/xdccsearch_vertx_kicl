package com.lxbluem.filenameresolver.domain.interactors;

import com.lxbluem.filenameresolver.domain.ports.incoming.CreateDownloadsDirectoryOnStartup;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystem;
import rx.Single;

public class CreateDownloadsDirectoryOnStartupImpl implements CreateDownloadsDirectoryOnStartup {
    private final FileSystem fileSystem;
    private final String downloadsPath;

    public CreateDownloadsDirectoryOnStartupImpl(FileSystem fileSystem, String downloadsPath) {
        this.fileSystem = fileSystem;
        this.downloadsPath = downloadsPath;
    }

    @Override
    public void execute() {
        fileSystem.fileOrDirExists(downloadsPath)
                .flatMap(exists -> {
                    if (Boolean.FALSE.equals(exists))
                        return fileSystem
                                .mkdir(downloadsPath);
                    return Single.just(null);
                })
                .subscribe();
    }
}
