package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoResponse;
import com.epam.digital.data.platform.management.model.dto.FileInfoResponse;

import java.util.List;

public interface VersionManagementService {

    /**
     * Get versions list
     */
    List<ChangeInfoResponse> getVersionsList();

    /**
     * Details of head master
     */
    List<String> getDetailsOfHeadMaster(String path);

    /**
     * Details of current version
     */
    List<FileInfoResponse> getVersionDetails(String versionName);

    /**
     * Create new version
     */
    void createNewVersion(String versionName);

    /**
     * Mark reviewed the version
     */
    boolean markReviewed(String versionName);

    /**
     * Submit version by name
     */
    void submit(String versionName);

    /**
     * Decline version by name
     */
    void decline(String versionName);

    /**
     * Rebase version
     */
    void rebase(String versionName);

    /**
     * Put votes to review
     */
    boolean vote(String versionName, String label, short value);

    /**
     * Add robot comment
     */
    void robotComment(String versionName, String robotId, String robotRunId, String comment, String message, String filePath);
}
