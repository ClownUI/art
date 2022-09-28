/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.art.model;

import static com.android.server.art.model.ArtFlags.OptimizeFlags;
import static com.android.server.art.model.ArtFlags.PriorityClassApi;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.text.TextUtils;

import com.android.server.art.ReasonMapping;
import com.android.server.art.Utils;

/** @hide */
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
public class OptimizeParams {
    public static final class Builder {
        private OptimizeParams mParams = new OptimizeParams();

        /**
         * Creates a builder.
         *
         * Uses default flags ({@link ArtFlags#defaultOptimizeFlags()}).
         *
         * @param reason See {@link #setReason(String)}.
         */
        public Builder(@NonNull String reason) {
            this(reason, ArtFlags.defaultOptimizeFlags());
        }

        /**
         * Same as above, but allows to specify flags.
         */
        public Builder(@NonNull String reason, @OptimizeFlags int flags) {
            setReason(reason);
            setFlags(flags);
        }

        /** Replaces all flags with the given value. */
        @NonNull
        public Builder setFlags(@OptimizeFlags int value) {
            mParams.mFlags = value;
            return this;
        }

        /** Replaces the flags specified by the mask with the given value. */
        @NonNull
        public Builder setFlags(@OptimizeFlags int value, @OptimizeFlags int mask) {
            mParams.mFlags = (mParams.mFlags & ~mask) | (value & mask);
            return this;
        }

        /**
         * The target compiler filter, passed as the {@code --compiler-filer} option to dex2oat.
         * Supported values are listed in
         * https://source.android.com/docs/core/dalvik/configure#compilation_options.
         *
         * Note that the compiler filter might be adjusted before the execution based on factors
         * like whether the profile is available or whether the app is used by other apps. If not
         * set, the default compiler filter for the given reason will be used.
         */
        @NonNull
        public Builder setCompilerFilter(@NonNull String value) {
            mParams.mCompilerFilter = value;
            return this;
        }

        /**
         * The priority of the operation. If not set, the default priority class for the given
         * reason will be used.
         *
         * @see PriorityClassApi
         */
        @NonNull
        public Builder setPriorityClass(@PriorityClassApi int value) {
            mParams.mPriorityClass = value;
            return this;
        }

        /**
         * Compilation reason. Can be a string defined in {@link ReasonMapping} or a custom string.
         *
         * If the value is a string defined in {@link ReasonMapping}, it determines the compiler
         * filter and/or the priority class, if those values are not explicitly set.
         *
         * If the value is a custom string, the priority class and the compiler filter must be
         * explicitly set.
         */
        @NonNull
        public Builder setReason(@NonNull String value) {
            mParams.mReason = value;
            return this;
        }

        /**
         * Returns the built object.
         *
         * @throws IllegalArgumentException if the built options would be invalid
         */
        @NonNull
        public OptimizeParams build() {
            if (mParams.mReason.isEmpty()) {
                throw new IllegalArgumentException("Reason must not be empty");
            }

            if (mParams.mCompilerFilter.isEmpty()) {
                mParams.mCompilerFilter = ReasonMapping.getCompilerFilterForReason(mParams.mReason);
            } else if (!Utils.isValidArtServiceCompilerFilter(mParams.mCompilerFilter)) {
                throw new IllegalArgumentException(
                        "Invalid compiler filter '" + mParams.mCompilerFilter + "'");
            }

            if (mParams.mPriorityClass == ArtFlags.PRIORITY_NONE) {
                mParams.mPriorityClass = ReasonMapping.getPriorityClassForReason(mParams.mReason);
            } else if (mParams.mPriorityClass < 0 || mParams.mPriorityClass > 100) {
                throw new IllegalArgumentException("Invalid priority class "
                        + mParams.mPriorityClass + ". Must be between 0 and 100");
            }

            return mParams;
        }
    }

    /**
     * A value indicating that dexopt shouldn't be run. This value is consumed by ART Services and
     * is not propagated to dex2oat.
     */
    public static final String COMPILER_FILTER_NOOP = "skip";

    private @OptimizeFlags int mFlags = 0;
    private @NonNull String mCompilerFilter = "";
    private @PriorityClassApi int mPriorityClass = ArtFlags.PRIORITY_NONE;
    private @NonNull String mReason = "";

    private OptimizeParams() {}

    /** Returns all flags. */
    public @OptimizeFlags int getFlags() {
        return mFlags;
    }

    /** The target compiler filter. */
    public @NonNull String getCompilerFilter() {
        return mCompilerFilter;
    }

    /** The priority class. */
    public @PriorityClassApi int getPriorityClass() {
        return mPriorityClass;
    }

    /**
     * The compilation reason.
     *
     * DO NOT directly use the string value to determine the resource usage and the process
     * priority. Use {@link #getPriorityClass}.
     */
    public @NonNull String getReason() {
        return mReason;
    }
}