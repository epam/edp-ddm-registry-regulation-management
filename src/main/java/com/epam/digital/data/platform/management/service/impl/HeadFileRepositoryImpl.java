package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.google.gerrit.extensions.restapi.RestApiException;
import lombok.Setter;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

@Setter
public class HeadFileRepositoryImpl implements VersionedFileRepository {

    private String versionName;

    private JGitService jGitService;

    @Override
    public void writeFile(String path, String content) throws Exception {
        throw new OperationNotSupportedException();
    }

    @Override
    public String readFile(String path) throws Exception {
        return jGitService.getFileContent(versionName, URLDecoder.decode(path, Charset.defaultCharset()));
    }

    @Override
    public boolean isFileExists(String path) throws Exception {
        File theFile = new File(path);
        String parent = theFile.getParent();
        return listFilesInHead(parent).stream().anyMatch(f -> theFile.getName().equals(f));
    }

    @Override
    public String deleteFile(String path) throws Exception {
        throw new OperationNotSupportedException();
    }

    @Override
    public String getVersionId() {
        return versionName;
    }

    @Override
    public void pullRepository() throws Exception {
        jGitService.cloneRepo(versionName);
    }

    private List<String> listFilesInHead(String path) throws Exception {
        return jGitService.getFilesInPath(versionName, path);
    }
}
