package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.exception.GitCommandException;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * Provides methods for working with git service.
 */
public interface JGitService {

  void cloneRepo(String versionName) throws Exception;

  /**
   * Fetches and resets repository for main branch to origin state
   *
   * @throws GitCommandException in case if it couldn't open repo or fetch or reset git command
   *                             failures
   */
  void resetHeadBranchToRemote();

  /**
   * Fetches and checkouts repository for specified version to remote state
   *
   * @param versionName   name of the specified version
   * @param changeInfoDto dto with ref info
   * @throws GitCommandException in case if it couldn't open repo or fetch or reset git command
   *                             failures
   */
  void fetch(@NonNull String versionName, @NonNull ChangeInfoDto changeInfoDto);

  List<String> getFilesInPath(String versionName, String path) throws Exception;

  FileDatesDto getDates(String versionName, String filePath);

  void formDatesCacheEvict();

  String getFileContent(String versionName, String filePath) throws Exception;

  String amend(VersioningRequestDto requestDto, ChangeInfoDto changeInfoDto) throws Exception;

  String delete(ChangeInfoDto changeInfoDto, String fileName) throws Exception;

  void deleteRepo(String repoName) throws IOException;
}
