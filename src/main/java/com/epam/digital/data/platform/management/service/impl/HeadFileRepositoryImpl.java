package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.model.dto.FormStatus;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter
public class HeadFileRepositoryImpl implements VersionedFileRepository {

    private String versionName;

    private JGitService jGitService;

    @Override
    public List<FormResponse> getFileList() throws Exception {
        return getFileList(File.pathSeparator);
    }

    @Override
    public List<FormResponse> getFileList(String path) throws Exception {
        Map<String, FormResponse> formsInMaster = jGitService.getFilesInPath(versionName, path).stream()
                .map(el -> FormResponse.builder().name(el).status(FormStatus.CURRENT).build())
                .collect(Collectors.toMap(FormResponse::getName, Function.identity()));

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
