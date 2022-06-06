package com.epam.digital.data.platform.management.mock;

import javax.annotation.PostConstruct;
import lombok.Getter;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultOperations;

@Component
public class VaultOperationsMock {

  @Getter
  private VaultOperations vaultOperations;

  @PostConstruct
  public void init() {
    this.vaultOperations = Mockito.mock(VaultOperations.class);
  }
}
