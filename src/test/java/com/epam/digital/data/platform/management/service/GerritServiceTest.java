package com.epam.digital.data.platform.management.service;
import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.RobotCommentRequestDto;
import com.epam.digital.data.platform.management.model.dto.VoteRequestDto;
import com.epam.digital.data.platform.management.service.impl.GerritServiceImpl;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import java.util.Collection;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.MergeableInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.urswolfer.gerrit.client.rest.GerritApiImpl;
import com.urswolfer.gerrit.client.rest.http.GerritRestClient;
import com.urswolfer.gerrit.client.rest.http.changes.ChangeApiRestClient;
import com.urswolfer.gerrit.client.rest.http.changes.ChangesRestClient;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
@ExtendWith(MockitoExtension.class)
public class GerritServiceTest {
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
    }
    @Test
    @SneakyThrows
    void getMrListNotNullTest(){
        Mockito.when(gerritApi.changes()).thenReturn(changes);
        Mockito.when(changes.query(any())).thenReturn(request);
        Mockito.when(request.get()).thenReturn(new ArrayList<>());
        List<ChangeInfo> mrList = gerritService.getMRList();
        Assertions.assertNotNull(mrList);
    }
    @Test
    @SneakyThrows
    void etMrListNotEmptyTest(){
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
        Mockito.when(gerritApi.changes()).thenReturn(changes);
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        gerritService.declineChange("changeId");
        Mockito.verify(changeApiRestClient, Mockito.times(1)).abandon();
    }
    @Test
    @SneakyThrows
    void robotCommentTest(){
        Mockito.when(gerritApi.changes()).thenReturn(changes);
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
    void robotCommentTestEmptyComment(){
        Mockito.when(gerritApi.changes()).thenReturn(changes);
        Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
        Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
        Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
        gerritService.rebase(null);
        RobotCommentRequestDto requestDto = new RobotCommentRequestDto();
        gerritService.robotComment(requestDto, "changeId");
        Mockito.verify(revisionApi, Mockito.times(1)).review(any());
    }
}
