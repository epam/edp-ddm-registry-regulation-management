package com.epam.digital.data.platform.management.forms;

import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import com.jayway.jsonpath.JsonPath;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Map versioned file dto to form dto
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface FormMapper {

  @Mapping(target = "created", source = "datesDto.create", defaultExpression ="java(fileInfoDto.getCreated())")
  @Mapping(target = "updated", source = "datesDto.update", defaultExpression ="java(fileInfoDto.getUpdated())")
  @Mapping(target = "title", source = "formContent", qualifiedByName = "getTitleFromFormContent")
  FormInfoDto toForm(VersionedFileInfoDto fileInfoDto, FileDatesDto datesDto, String formContent, boolean conflicted);

  @Named("getTitleFromFormContent")
  default String getTitleFromFormContent(String formContent) {
    return JsonPath.read(formContent, "$.title");
  }
}
