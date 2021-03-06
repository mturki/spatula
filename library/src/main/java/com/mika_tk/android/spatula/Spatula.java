/*
 * Copyright  2017. Mikhaël Turki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.mika_tk.android.spatula;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;

import java.lang.reflect.Field;


/**
 * Created by Mikhael Turki on 04/01/16.
 *
 * Class used to apply Typefaces on android View objects that extends {@link TextView} like {@link android.widget.Button}, {@link android.widget.ToggleButton} or extends {@link TextInputLayout}
 * It can be user as tool class or with annotations like Butterknife.
 * <p>
 * call {@link #with(TypefaceMapper)} before binding. For example in your {@link android.app.Application} put :
 * <p>
 * <meta-data
 * android:name="com.mika_tk.android.spatula.Mapper"
 * android:value="your.mapper.implem.class"/>
 *
 * @see <a href="https://github.com/mturki/Spatula/blob/master/README.md">https://github.com/mturki/Spatula/blob/master/README.md</a> for help.
 */
public final class Spatula {

    private static final String TAG = Spatula.class.getSimpleName();

    /**
     * meta-data String Name. Use it to declare your {@Link #TypefaceMapper} implementation for config.
     */
    public static final String META_DATA = "com.mika_tk.android.spatula.Mapper";

    /**
     * Typeface "cache".
     */
    private static SparseArray<Typeface> sTypefacesCacheList = new SparseArray<>(5);

    /**
     * The font mapper Ref.
     */
    private static TypefaceMapper sTypefaceMapper;

    /**
     * If we tried to initialize  it at least once.
     */
    private static boolean sIsInit = false;

    private static final String ERROR_INIT = "you have to declare a meta-data item in manifest to configure the font mapper, or instantiate it at least one time by calling Spatula.with(...) function";

    private Spatula() {
    }

    /**
     * Set or change the current {@link TypefaceMapper} implementation
     *
     * @param mapper the font mapper to set
     */
    public static void with(@NonNull TypefaceMapper mapper) {
        Log.i(TAG, "Spatula init");
        sTypefaceMapper = mapper;
        sIsInit = true;
    }

    @NonNull
    private static TypefaceMapper getMapperFromMetaData(@NonNull Context context) throws RuntimeException {
        Bundle bundle = null;
        try {
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            bundle = app.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "meta name = " + META_DATA);
        if (bundle == null || !bundle.containsKey(META_DATA)) {
            throw new IllegalArgumentException(ERROR_INIT);
        }
        String mapperClassName = bundle.getString(META_DATA);
        Log.d(TAG, "mapperClassName = " + mapperClassName);
        return parseMapper(mapperClassName);
    }

    /**
     * parse meta by name.
     * thanks to GlideModule parser  : {
     * https://github.com/bumptech/glide/blob/3bcea91b7709ad219887fb9b71dff9222dbd3d99/library/src/main/java/com/bumptech/glide/module/ManifestParser.java
     * }
     *
     * @param className the parser module name
     * @return
     */
    private static TypefaceMapper parseMapper(String className) throws RuntimeException {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.getMessage());
            throw new IllegalArgumentException("Unable to find FontMapper implementation", e);
        }

        Object module;
        //noinspection TryWithIdenticalCatches
        try {
            module = clazz.newInstance();
        } catch (InstantiationException e) {
            Log.e(TAG, e.getMessage());
            throw new RuntimeException("Unable to instantiate FontMapper implementation for " + clazz,
                    e);
            // These can't be combined until API minimum is 19.
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage());
            throw new RuntimeException("Unable to instantiate FontMapper implementation for " + clazz,
                    e);
        }

        if (!(module instanceof TypefaceMapper)) {
            Log.e(TAG, "Expected instanceof FontMapper, but found: ");
            throw new RuntimeException("Expected instanceof FontMapper, but found: " + module);
        }
        return (TypefaceMapper) module;
    }

    /**
     * call this method to bind typefaces view that extends
     * {@link TextView} like {@link TextView}, {@link android.widget.Button}, {@link android.widget.ToggleButton} annotated with {@link BindTypeface}
     *
     * @param parent the parent object. Can be an {@link android.app.Activity} or a {@link android.support.v4.app.Fragment} or any kind of wrapping
     *               classes or view holder.
     */
    @SuppressWarnings("unused")
    public static void bindTypefaces(@NonNull Object parent) {
        final Field[] fields = parent.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(BindTypeface.class)) {
                try {
                    field.setAccessible(true);
                    BindTypeface applyTypeFace = field.getAnnotation(BindTypeface.class);
                    int typeFaceIndex = applyTypeFace.value();
                    if (TextView.class.isAssignableFrom(field.getType())) {
                        TextView textview = (TextView) field.get(parent);
                        apply(typeFaceIndex, textview);
                    } else if (TextInputLayout.class.isAssignableFrom(field.getType())) {
                        TextInputLayout textInputLayout = (TextInputLayout) field.get(parent);
                        Log.v(TAG, "apply typeface : " + typeFaceIndex);
                        apply(typeFaceIndex, textInputLayout);
                    } else {
                        Log.e(TAG, "can't apply typeface to " + field.getType());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "can't apply typeface");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Apply a typeface to a single object
     *
     * @param typefaceIndex the typeface to apply
     * @param v             the TextInputLayout to apply to
     */
    private static void apply(int typefaceIndex, TextInputLayout v) {
        if (v != null && v.getContext() != null) {
            Typeface type = getTypeFace(v.getContext().getApplicationContext(), typefaceIndex);
            if (type != null) {
                Log.v(TAG, "Apply typeface : " + type.toString());
                v.setTypeface(type);
            } else if (TypefaceMapper.regular != typefaceIndex) {
                Log.w(TAG, String.format("Can't locate typeface {%d}. retry with default 'regular' typeface : ", typefaceIndex));
                apply(v);
            }

        }
    }

    /**
     * Apply a typeface to a single object
     *
     * @param typefaceIndex the typeface to apply
     * @param v             the textView to apply to
     */
    private static void apply(int typefaceIndex, TextView v) {
        if (v != null && v.getContext() != null) {
            Typeface type = getTypeFace(v.getContext().getApplicationContext(), typefaceIndex);
            if (type != null) {
                Log.v(TAG, "Apply typeface : " + type.toString());
                v.setTypeface(type);
            } else if (TypefaceMapper.regular != typefaceIndex) {
                Log.w(TAG, String.format("Can't locate typeface {%d}. Trying to apply 'regular' typeface : ", typefaceIndex));
                apply(v);
            }
        }
    }

    /**
     * @param typefaceIndex the typeface index to apply
     * @param v             collection of {@link TextView} to apply typeface to
     */
    public static void apply(int typefaceIndex, TextView... v) {
        for (TextView textView : v) {
            apply(typefaceIndex, textView);
        }
    }

    /**
     * apply the default typeface {@link TypefaceMapper#regular} the the given {@link TextView}
     *
     * @param v the text view
     */
    public static void apply(TextView... v) {
        apply(TypefaceMapper.regular, v);
    }

    /**
     * @param typefaceIndex the typeface index to apply
     * @param v             collection of {@link TextInputLayout} to apply to
     */
    public static void apply(int typefaceIndex, TextInputLayout... v) {
        for (TextInputLayout textView : v) {
            apply(typefaceIndex, textView);
        }
    }

    /**
     * Apply the default typeface {@link TypefaceMapper#regular} the the given {@link TextInputLayout}
     *
     * @param v the text view
     */
    public static void apply(TextInputLayout... v) {
        apply(TypefaceMapper.regular, v);
    }

    /**
     * @param ctx       the android context. can't be null
     * @param fontIndex the typeface index
     * @return the Typeface object
     */
    @Nullable
    private static synchronized Typeface getTypeFace(@NonNull Context ctx, int fontIndex) {
        String fontName = null;
        Typeface typeFace = sTypefacesCacheList.get(fontIndex);
        if (typeFace != null) {
            Log.d(TAG, String.format("typeface at %d : retrieved from cache.", fontIndex));
            return typeFace;
        } else if (!sIsInit && sTypefaceMapper == null) {
            initFromMetaData(ctx);
        } else if (sIsInit && sTypefaceMapper == null) {
            Log.e(TAG, ERROR_INIT);
            return null;
        }

        try {
            fontName = sTypefaceMapper.getTypefaceName(fontIndex);
            Log.i(TAG, "fontName = " + fontName);
            typeFace = Typeface.createFromAsset(ctx.getAssets(), "fonts/" + fontName);
            Log.i(TAG, "createFromAsset is OK for " + typeFace.toString());
        } catch (NullPointerException e) {
            Log.e(TAG, e.getMessage());
            Log.e(TAG, String.format("can't apply typeface %s : this font is not defined in your FontMapper implementation.", fontName));
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
            Log.e(TAG, String.format("can't apply typeface : %s not in 'assets/fonts/' folder", fontName));
        } finally {
            Log.i(TAG, "fontName = " + fontName + " added to cache");
            sTypefacesCacheList.put(fontIndex, typeFace);
        }
        return typeFace;
    }

    private static void initFromMetaData(Context context) {
        //get mapper from meta data if null (just once);
        Log.i(TAG, "Spatula init");
        sTypefaceMapper = getMapperFromMetaData(context);
        sIsInit = true;
    }
}
