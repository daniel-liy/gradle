/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.tooling.internal.protocol;

/**
 * Identity information for a model.
 *
 * <p>DO NOT CHANGE THIS INTERFACE - it is part of the cross-version protocol.
 *
 * <p>Consumer compatibility: This interface is used by all consumer versions from 1.6-rc-1.</p> <p>Provider compatibility: This interface is uses by all provider versions from 1.6-rc-1.</p>
 *
 * @since 1.6-rc-1
 */
public interface ModelIdentifier {
    /**
     * The name of the null model.
     */
    final String NULL_MODEL = Void.class.getName();

    /**
     * The name of the model.
     *
     * Note that the model name is not necessarily a class name, and it simply uniquely identifies the model.
     * Use {@link #NULL_MODEL} to indicate that no model is desired.
     */
    String getName();

    /**
     * The version of the model. May be null.
     */
    String getVersion();
}
