package com.ticketseller.app.ui.screens;

import com.ticketseller.app.data.api.ApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class HomeVm_Factory implements Factory<HomeVm> {
  private final Provider<ApiService> apiProvider;

  public HomeVm_Factory(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public HomeVm get() {
    return newInstance(apiProvider.get());
  }

  public static HomeVm_Factory create(Provider<ApiService> apiProvider) {
    return new HomeVm_Factory(apiProvider);
  }

  public static HomeVm newInstance(ApiService api) {
    return new HomeVm(api);
  }
}
