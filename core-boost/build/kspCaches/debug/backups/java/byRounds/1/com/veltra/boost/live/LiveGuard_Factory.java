package com.veltra.boost.live;

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
public final class LiveGuard_Factory implements Factory<LiveGuard> {
  private final Provider<Context> ctxProvider;

  public LiveGuard_Factory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public LiveGuard get() {
    return newInstance(ctxProvider.get());
  }

  public static LiveGuard_Factory create(Provider<Context> ctxProvider) {
    return new LiveGuard_Factory(ctxProvider);
  }

  public static LiveGuard newInstance(Context ctx) {
    return new LiveGuard(ctx);
  }
}
