package com.zz.gallery;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Created by Zzisok001 on 2017/11/22.
 */
@GlideModule
public final class GlideAppModule extends AppGlideModule {
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
