package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.naming.OperationNotSupportedException;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

@Setter
public class HeadFileRepositoryImpl implements VersionedFileRepository {

    private String versionName;

    private JGitService jGitService;

    @Override
    public List<FileResponse> getFileList() throws Exception {
        return getFileList(File.pathSeparator);
    }

    @Override
    public List<FileResponse> getFileList(String path) throws Exception {
        Map<String, FileResponse> formsInMaster = jGitService.getFilesInPath(versionName, path)
            .stream()
            .map(el -> FileResponse.builder()
                .name(FilenameUtils.getBaseName(el))
                .status(FileStatus.CURRENT)
                .path(path)
                .build())
            .collect(Collectors.toMap(FileResponse::getName, Function.identity()));

        return new ArrayList<>(formsInMaster.values());
    }

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
