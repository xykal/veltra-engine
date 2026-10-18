package com.veltra.data;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class GameRepository_Factory implements Factory<GameRepository> {
  private final Provider<Context> ctxProvider;

  public GameRepository_Factory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public GameRepository get() {
    return newInstance(ctxProvider.get());
  }

  public static GameRepository_Factory create(Provider<Context> ctxProvider) {
    return new GameRepository_Factory(ctxProvider);
  }

  public static GameRepository newInstance(Context ctx) {
    return new GameRepository(ctx);
  }
}
