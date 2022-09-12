package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import java.io.IOException;
import java.util.List;

public interface JGitService {

  void cloneRepo(String versionName) throws Exception;

  void pull(String versionName) throws Exception;

  void fetch(String versionName, ChangeInfoDto changeInfoDto) throws Exception;

  List<String> getFilesInPath(String versionName, String path) throws Exception;

  FileDatesDto getDates(String versionName, String filePath);

  void formDatesCacheEvict();

  String getFileContent(String versionName, String filePath) throws Exception;

  String amend(VersioningRequestDto requestDto, ChangeInfoDto changeInfoDto) throws Exception;

  String delete(ChangeInfoDto changeInfoDto, String fileName) throws Exception;

  void deleteRepo(String repoName) throws IOException;
}
