package com.epam.digital.data.platform.management.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.RobotCommentRequestDto;
import com.epam.digital.data.platform.management.model.dto.VoteRequestDto;
import com.epam.digital.data.platform.management.service.impl.GerritServiceImpl;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.MergeableInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.urswolfer.gerrit.client.rest.GerritApiImpl;
import com.urswolfer.gerrit.client.rest.http.GerritRestClient;
import com.urswolfer.gerrit.client.rest.http.changes.ChangeApiRestClient;
import com.urswolfer.gerrit.client.rest.http.changes.ChangesRestClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GerritServiceTest {

    @Mock
    public GerritPropertiesConfig gerritPropertiesConfig;
    @Mock
    public GerritApi gerritApi;
    @Mock
    public GerritApiImpl gerritApiImpl;
    @InjectMocks
    private GerritServiceImpl gerritService;
    @Mock
    private ChangesRestClient changes;
    @Mock
    private ChangeApiRestClient changeApiRestClient;
    @Mock
    private Changes.QueryRequest request;
    @Mock
    private RevisionApi revisionApi;
    @Mock
    private GerritRestClient gerritRestClient;
    List<ChangeInfo> changeInfos = new ArrayList<>();
    ChangeInfo changeInfo = new ChangeInfo();

    @BeforeEach
    void initChanges() {
        changeInfo._number = 5;
        changeInfos.add(changeInfo);
        Mockito.lenient().when(gerritApi.changes()).thenReturn(changes);
    }

    @Test
    @SneakyThrows
    void getTopicFromChange() {

    }

    @Test
    @SneakyThrows
    void writeAndGetTopicFromChangeTest() {
        String topic1 = "testTopic1";
        String topic2 = "testTopic2";

        ChangeApi changeApi = Mockito.mock(ChangeApi.class);
        Mockito.when(changes.id(any())).thenReturn(changeApi);

        ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
        Mockito.when(changeApi.topic()).thenAnswer(e -> valueCapture.getValue());
        Mockito.doNothing().when(changeApi).topic(valueCapture.capture());

        gerritService.setTopic(topic1, "");
        Assertions.assertEquals(topic1, valueCapture.getValue());
        String rTopic1 = gerritService.getTopic("");
        gerritService.setTopic(topic2, "");
        Assertions.assertEquals(topic2, valueCapture.getValue());
        String rTopic2 = gerritService.getTopic("");

        Assertions.assertEquals(topic1, rTopic1);
        Assertions.assertEquals(topic2, rTopic2);
    }

    @Test
    @SneakyThrows
    void getMRByNumberTest() {
        Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
        String versionNumber = "10";
        String testVersion = "project:+" + versionNumber;
        ChangeInfo info = new ChangeInfo();
        info._number = 10;
        info.mergeable = true;
        Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
        ChangeApi changeApi = Mockito.mock(ChangeApi.class);
        RevisionApi revisionApi = Mockito.mock(RevisionApi.class);
        MergeableInfo mergeableInfo = Mockito.mock(MergeableInfo.class);
        mergeableInfo.mergeable = false;
        Mockito.when(changes.id(any())).thenReturn(changeApi);
        Mockito.when(changeApi.get()).thenReturn(info);
        Mockito.when(changeApi.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.mergeable()).thenReturn(mergeableInfo);
        Mockito.when(request.get()).thenReturn(changeInfos);
        Assertions.assertTrue(info.mergeable);
        ChangeInfo c = gerritService.getMRByNumber(versionNumber);
        Assertions.assertFalse(info.mergeable);
        Assertions.assertEquals(info, c);
    }

    @Test
    @SneakyThrows
    void getMRByNumberNullableTest() {
        Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
        String versionNumber = "10";
        String testVersion = "project:+" + versionNumber;
        ChangeInfo info = new ChangeInfo();
        info._number = 10;
        info.mergeable = true;
        Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
        ChangeApi changeApi = Mockito.mock(ChangeApi.class);
        RevisionApi revisionApi = Mockito.mock(RevisionApi.class);
        MergeableInfo mergeableInfo = Mockito.mock(MergeableInfo.class);
        mergeableInfo.mergeable = false;
        Mockito.when(request.get()).thenReturn(new ArrayList<>());
        ChangeInfo c = gerritService.getMRByNumber(versionNumber);
        Assertions.assertNull(c);
    }

    @Test
    @SneakyThrows
    void getMrListNotNullTest(){
        Mockito.when(changes.query(any())).thenReturn(request);
        Mockito.when(request.get()).thenReturn(new ArrayList<>());
        List<ChangeInfo> mrList = gerritService.getMRList();
        Assertions.assertNotNull(mrList);
    }
    @Test
    @SneakyThrows
    void etMrListNotEmptyTest(){
        Mockito.when(changes.query(any())).thenReturn(request);
        Mockito.when(request.get()).thenReturn(changeInfos);
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
        Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.mergeable()).thenReturn(new MergeableInfo());
        List<ChangeInfo> mrList = gerritService.getMRList();
        Assertions.assertNotNull(mrList);
        ChangeInfo changeInfo = mrList.get(0);
        Assertions.assertEquals(5, changeInfo._number);
    }
    @Test
    @SneakyThrows
    void getChangeInfoTest() {
        RevisionInfo revisionInfo = new RevisionInfo();
        revisionInfo.ref = "refs";
        HashMap<String, RevisionInfo> revisionsMap = new HashMap<>();
        revisionsMap.put(null, revisionInfo);
        changeInfo.revisions = revisionsMap;
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
        ChangeInfoDto changeId = gerritService.getChangeInfo("changeId");
        Assertions.assertEquals("changeId", changeId.getChangeId());
        Assertions.assertEquals("refs", changeId.getRefs());
        Assertions.assertEquals("5", changeId.getNumber());
    }
    @Test
    @SneakyThrows
    void getMrChangesListTest(){
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.files()).thenReturn(new HashMap<>());
        Map<String, FileInfo> files = gerritService.getListOfChangesInMR("changeId");
        Assertions.assertNotNull(files);
    }
    @Test
    @SneakyThrows
    void getFileContentTest(){
        String fileContent = gerritService.getFileContent(null, "");
        Assertions.assertNull(fileContent);
    }
    @Test
    @SneakyThrows
    void createChangesTest(){
        Mockito.when(changes.create(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
        String change = gerritService.createChanges("change");
        Assertions.assertNotNull(change);
        Assertions.assertEquals("5", change);
    }
    @Test
    @SneakyThrows
    void reviewTest(){
        ReviewResult reviewResult = new ReviewResult();
        reviewResult.ready = true;
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.review(any())).thenReturn(reviewResult);
        Boolean review = gerritService.review("changeId");
        Assertions.assertNotNull(review);
    }
    @Test
    @SneakyThrows
    void voteTestIfLabelOrValueNull(){
        Boolean vote = gerritService.vote(new VoteRequestDto(), "changeId");
        Assertions.assertEquals(false, vote);
        VoteRequestDto voteRequestDto = new VoteRequestDto();
        voteRequestDto.setLabel("Code-Review");
        vote = gerritService.vote(voteRequestDto, "changeId");
        Assertions.assertEquals(false, vote);
        voteRequestDto = new VoteRequestDto();
        voteRequestDto.setValue((short) 2);
        vote = gerritService.vote(voteRequestDto, "changeId");
        Assertions.assertEquals(false, vote);
    }
    @Test
    @SneakyThrows
    void voteTestIfPermittedLabelsAreEmpty() {
        Map<String, Collection<String>> labels = new HashMap<>();
        List<String> vals = new ArrayList<>();
        labels.put("Code-Review", null);
        changeInfo.permittedLabels = labels;
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
        VoteRequestDto dto = new VoteRequestDto();
        dto.setLabel("Code-Review");
        dto.setValue((short) 0);
        Boolean vote = gerritService.vote(dto, "changeId");
        Assertions.assertEquals(false, vote);
    }
    @Test
    @SneakyThrows
    void voteTest(){
        Map<String, Collection<String>> labels = new HashMap<>();
        List<String> vals = new ArrayList<>();
        vals.add("+2");
        labels.put("Code-Review", vals);
        changeInfo.permittedLabels = labels;
        ReviewResult reviewResult = new ReviewResult();
        reviewResult.ready = true;
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
        Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.review(any())).thenReturn(reviewResult);
        VoteRequestDto dto = new VoteRequestDto();
        dto.setLabel("Code-Review");
        dto.setValue((short) 2);
        Boolean vote = gerritService.vote(dto, "changeId");
        Assertions.assertEquals(true, vote);
    }
    @Test
    @SneakyThrows
    void declineChangeTest(){
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        gerritService.declineChange("changeId");
        Mockito.verify(changeApiRestClient, Mockito.times(1)).abandon();
    }
    @Test
    @SneakyThrows
    void robotCommentTest(){
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
        gerritService.rebase(null);
        RobotCommentRequestDto requestDto = new RobotCommentRequestDto();
        requestDto.setComment("Comment");
        gerritService.robotComment(requestDto, "changeId");
        Mockito.verify(revisionApi, Mockito.times(1)).review(any());
    }

    @Test
    @SneakyThrows
    void robotCommentTestEmptyComment() {
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
        gerritService.rebase(null);
        RobotCommentRequestDto requestDto = new RobotCommentRequestDto();
        gerritService.robotComment(requestDto, "changeId");
        Mockito.verify(revisionApi, Mockito.times(1)).review(any());
    }

    @Test
    @SneakyThrows
    void getLastMergedMR() {
        Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
        Mockito.when(changes.query("project:repo+status:merged")).thenReturn(request);
        Mockito.when(request.withLimit(1)).thenReturn(request);
        Mockito.when(request.get()).thenReturn(changeInfos);
        Mockito.when(changes.id(changeInfo.id)).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);

        var result = gerritService.getLastMergedMR();

        Assertions.assertSame(result, changeInfo);

        Mockito.verify(request).get();
        Mockito.verify(request).withLimit(1);
        Mockito.verify(changes).query("project:repo+status:merged");
        Mockito.verify(gerritPropertiesConfig).getRepository();
    }

    @Test
    @SneakyThrows
    void getLastMergedMR_noMergedMRs() {
        Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
        Mockito.when(changes.query("project:repo+status:merged")).thenReturn(request);
        Mockito.when(request.withLimit(1)).thenReturn(request);
        Mockito.when(request.get()).thenReturn(List.of());

        var result = gerritService.getLastMergedMR();

        Assertions.assertNull(result);

        Mockito.verify(request).get();
        Mockito.verify(request).withLimit(1);
        Mockito.verify(changes).query("project:repo+status:merged");
        Mockito.verify(gerritPropertiesConfig).getRepository();
    }
}
