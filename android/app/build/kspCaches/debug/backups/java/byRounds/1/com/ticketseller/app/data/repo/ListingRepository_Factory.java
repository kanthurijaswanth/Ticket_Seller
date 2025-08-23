package com.ticketseller.app.data.repo;

import com.ticketseller.app.data.api.ApiService;
import com.ticketseller.app.data.db.AppDb;
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
public final class ListingRepository_Factory implements Factory<ListingRepository> {
  private final Provider<ApiService> apiProvider;

  private final Provider<AppDb> dbProvider;

  public ListingRepository_Factory(Provider<ApiService> apiProvider, Provider<AppDb> dbProvider) {
    this.apiProvider = apiProvider;
    this.dbProvider = dbProvider;
  }

  @Override
  public ListingRepository get() {
    return newInstance(apiProvider.get(), dbProvider.get());
  }

  public static ListingRepository_Factory create(Provider<ApiService> apiProvider,
      Provider<AppDb> dbProvider) {
    return new ListingRepository_Factory(apiProvider, dbProvider);
  }

  public static ListingRepository newInstance(ApiService api, AppDb db) {
    return new ListingRepository(api, db);
  }
}
