package com.ticketseller.app.di;

import android.content.Context;
import com.ticketseller.app.data.db.AppDb;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DbModule_DbFactory implements Factory<AppDb> {
  private final Provider<Context> ctxProvider;

  public DbModule_DbFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public AppDb get() {
    return db(ctxProvider.get());
  }

  public static DbModule_DbFactory create(Provider<Context> ctxProvider) {
    return new DbModule_DbFactory(ctxProvider);
  }

  public static AppDb db(Context ctx) {
    return Preconditions.checkNotNullFromProvides(DbModule.INSTANCE.db(ctx));
  }
}
