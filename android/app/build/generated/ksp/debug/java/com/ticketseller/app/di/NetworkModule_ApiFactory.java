package com.ticketseller.app.di;

import com.ticketseller.app.data.api.ApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ApiFactory implements Factory<ApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ApiService get() {
    return api(retrofitProvider.get());
  }

  public static NetworkModule_ApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ApiFactory(retrofitProvider);
  }

  public static ApiService api(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.api(retrofit));
  }
}
