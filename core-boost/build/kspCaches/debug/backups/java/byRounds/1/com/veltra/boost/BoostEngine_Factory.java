package com.veltra.boost;

import android.content.Context;
import com.veltra.boost.live.LiveGuard;
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
public final class BoostEngine_Factory implements Factory<BoostEngine> {
  private final Provider<Context> ctxProvider;

  private final Provider<LiveGuard> liveGuardProvider;

  public BoostEngine_Factory(Provider<Context> ctxProvider, Provider<LiveGuard> liveGuardProvider) {
    this.ctxProvider = ctxProvider;
    this.liveGuardProvider = liveGuardProvider;
  }

  @Override
  public BoostEngine get() {
    return newInstance(ctxProvider.get(), liveGuardProvider.get());
  }

  public static BoostEngine_Factory create(Provider<Context> ctxProvider,
      Provider<LiveGuard> liveGuardProvider) {
    return new BoostEngine_Factory(ctxProvider, liveGuardProvider);
  }

  public static BoostEngine newInstance(Context ctx, LiveGuard liveGuard) {
    return new BoostEngine(ctx, liveGuard);
  }
}
