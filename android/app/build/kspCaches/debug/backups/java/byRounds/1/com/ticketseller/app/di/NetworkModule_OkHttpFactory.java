package com.ticketseller.app.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NetworkModule_OkHttpFactory implements Factory<OkHttpClient> {
  @Override
  public OkHttpClient get() {
    return okHttp();
  }

  public static NetworkModule_OkHttpFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OkHttpClient okHttp() {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.okHttp());
  }

  private static final class InstanceHolder {
    private static final NetworkModule_OkHttpFactory INSTANCE = new NetworkModule_OkHttpFactory();
  }
}
