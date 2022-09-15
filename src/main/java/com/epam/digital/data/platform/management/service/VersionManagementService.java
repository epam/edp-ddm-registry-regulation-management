package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.VersionChanges;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.util.List;
import org.springframework.lang.Nullable;

public interface VersionManagementService {

    /**
     * Get versions list
     */
    List<ChangeInfoDetailedDto> getVersionsList() throws RestApiException;


    @Nullable
    ChangeInfoDetailedDto getMasterInfo() throws RestApiException;

    /**
     * Details of head master
     */
    List<String> getDetailsOfHeadMaster(String path) throws Exception;

    /**
     * Details of current version
     */
    List<VersionedFileInfo> getVersionFileList(String versionName) throws Exception;

    /**
     * Create new version
     */
    String createNewVersion(CreateVersionRequest subject) throws RestApiException;

    ChangeInfoDetailedDto getVersionDetails(String versionName) throws RestApiException;

    /**
     * Decline version by name
     */
    void decline(String versionName);

    /**
     * Mark reviewed the version
     */
    boolean markReviewed(String versionName);

    /**
     * Submit version by name
     */
    void submit(String versionName);
//
//    /**
//     * Rebase version
//     */
//    void rebase(String versionName);
//
//    /**
//     * Put votes to review
//     */
    boolean vote(String versionName, String label, short value) throws RestApiException;

    VersionChanges getVersionChanges(String versionCandidateId);
//
//    /**
//     * Add robot comment
//     */
//    void robotComment(String versionName, String robotId, String robotRunId, String comment, String message, String filePath);
}
